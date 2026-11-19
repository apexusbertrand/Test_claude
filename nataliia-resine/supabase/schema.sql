-- Nataliia Résine : schéma Supabase (lot 2, administration)
-- À exécuter une fois dans l'éditeur SQL du projet Supabase (région UE).
-- Idempotent : peut être relancé sans perte de données.

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- Administrateurs
-- Un compte Supabase Auth n'a accès à l'administration que s'il est listé ici.
-- ---------------------------------------------------------------------------
create table if not exists public.admins (
  user_id uuid primary key references auth.users (id) on delete cascade,
  email text not null,
  created_at timestamptz not null default now()
);

create or replace function public.is_admin()
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (select 1 from public.admins where user_id = auth.uid());
$$;

-- ---------------------------------------------------------------------------
-- Contenus du site
-- Une photo = objet JSON {"path": "realisations/xxx.webp", "small": "realisations/xxx-800.webp", "alt": "..."}
-- Les chemins sont relatifs au bucket public « media ».
-- ---------------------------------------------------------------------------
create table if not exists public.reglages (
  cle text primary key,
  valeur jsonb not null,
  updated_at timestamptz not null default now()
);

create table if not exists public.prestations (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  titre text not null check (length(trim(titre)) > 0),
  resume text not null default '',
  texte text not null default '',
  photo jsonb,
  ordre integer not null default 0,
  publie boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.produits (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  nom text not null check (length(trim(nom)) > 0),
  categorie text not null check (categorie in ('granulats', 'profils', 'autre')),
  descriptif text not null default '',
  photos jsonb not null default '[]'::jsonb check (jsonb_typeof(photos) = 'array'),
  lien text,
  pdf jsonb,
  texture jsonb,
  grain_mm numeric(5, 1) check (grain_mm is null or grain_mm > 0),
  exemple boolean not null default false,
  ordre integer not null default 0,
  publie boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.realisations (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  titre text not null check (length(trim(titre)) > 0),
  type text not null,
  ville text,
  texte text not null default '',
  apres jsonb not null default '[]'::jsonb check (jsonb_typeof(apres) = 'array' and jsonb_array_length(apres) between 1 and 4),
  avant jsonb not null default '[]'::jsonb check (jsonb_typeof(avant) = 'array' and jsonb_array_length(avant) <= 2),
  produits uuid[] not null default '{}',
  date_chantier date,
  ordre integer not null default 0,
  publie boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.avis (
  id uuid primary key default gen_random_uuid(),
  nom text not null check (length(trim(nom)) > 0),
  ville text,
  texte text not null check (length(trim(texte)) > 0),
  date_avis date,
  ordre integer not null default 0,
  publie boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Demandes des prospects (remplies par le simulateur au lot 3)
-- Jamais lisibles publiquement.
-- ---------------------------------------------------------------------------
create table if not exists public.prospects (
  id uuid primary key default gen_random_uuid(),
  nom text not null,
  email text not null,
  telephone text not null,
  ville text,
  statut text not null default 'nouveau'
    check (statut in ('nouveau', 'contacte', 'devis', 'signe', 'sans_suite')),
  notes text not null default '',
  dernier_contact timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create unique index if not exists prospects_email_idx on public.prospects (lower(email));
-- E-mails toujours en minuscules (la fonction « demande » s'appuie dessus pour retrouver un dossier)
do $$ begin
  alter table public.prospects add constraint prospects_email_minuscules check (email = lower(email));
exception when duplicate_object then null;
end $$;

create table if not exists public.demandes (
  id uuid primary key default gen_random_uuid(),
  prospect_id uuid not null references public.prospects (id) on delete cascade,
  type_surface text,
  surface_m2 numeric(8, 1),
  delai text,
  message text,
  photos jsonb not null default '[]'::jsonb, -- [{"original": "...", "rendu": "...", "produit_id": "..."}] dans le bucket privé « demandes »
  consentement_at timestamptz not null,
  created_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Historique des modifications (pour revenir en arrière) et publications
-- ---------------------------------------------------------------------------
create table if not exists public.historique (
  id bigint generated always as identity primary key,
  table_name text not null,
  row_id text not null,
  action text not null check (action in ('INSERT', 'UPDATE', 'DELETE')),
  avant jsonb,
  apres jsonb,
  auteur text,
  at timestamptz not null default now()
);
create index if not exists historique_at_idx on public.historique (at desc);

create table if not exists public.publications (
  id bigint generated always as identity primary key,
  demandee_par text,
  statut text not null default 'envoyee',
  detail text,
  at timestamptz not null default now()
);

-- updated_at automatique
create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;

-- Journal : une ligne par modification, avec l'auteur
create or replace function public.journaliser()
returns trigger
language plpgsql security definer
set search_path = public
as $$
declare
  rid text;
  qui text;
begin
  rid := coalesce(
    case when tg_op = 'DELETE' then to_jsonb(old) else to_jsonb(new) end ->> 'id',
    case when tg_op = 'DELETE' then to_jsonb(old) else to_jsonb(new) end ->> 'cle'
  );
  select email into qui from public.admins where user_id = auth.uid();
  insert into public.historique (table_name, row_id, action, avant, apres, auteur)
  values (
    tg_table_name, rid, tg_op,
    case when tg_op in ('UPDATE', 'DELETE') then to_jsonb(old) end,
    case when tg_op in ('INSERT', 'UPDATE') then to_jsonb(new) end,
    coalesce(qui, 'système')
  );
  return null;
end $$;

do $$
declare t text;
begin
  foreach t in array array['reglages', 'prestations', 'produits', 'realisations', 'avis', 'prospects'] loop
    execute format('drop trigger if exists %I on public.%I', t || '_touch', t);
    execute format('create trigger %I before update on public.%I for each row execute function public.touch_updated_at()', t || '_touch', t);
  end loop;
  foreach t in array array['reglages', 'prestations', 'produits', 'realisations', 'avis'] loop
    execute format('drop trigger if exists %I on public.%I', t || '_journal', t);
    execute format('create trigger %I after insert or update or delete on public.%I for each row execute function public.journaliser()', t || '_journal', t);
  end loop;
end $$;

-- Purge RGPD des fiches (sans les photos). En production, utiliser la fonction Supabase « purger »,
-- qui supprime aussi les photos du stockage puis les fiches. Celle-ci sert aux tests et en secours.
create or replace function public.purger_prospects()
returns integer
language plpgsql security definer
set search_path = public
as $$
declare n integer;
begin
  delete from public.prospects
  where statut <> 'signe' and dernier_contact < now() - interval '3 years';
  get diagnostics n = row_count;
  return n;
end $$;

-- ---------------------------------------------------------------------------
-- Droits (Row Level Security)
-- anon : lecture des contenus publiés uniquement. Admins : tout.
-- ---------------------------------------------------------------------------
alter table public.admins enable row level security;
alter table public.reglages enable row level security;
alter table public.prestations enable row level security;
alter table public.produits enable row level security;
alter table public.realisations enable row level security;
alter table public.avis enable row level security;
alter table public.prospects enable row level security;
alter table public.demandes enable row level security;
alter table public.historique enable row level security;
alter table public.publications enable row level security;

do $$
declare t text;
begin
  -- Contenus publics : lecture des lignes publiées par tous, écriture par les admins
  foreach t in array array['prestations', 'produits', 'realisations', 'avis'] loop
    execute format('drop policy if exists lecture_publique on public.%I', t);
    execute format('create policy lecture_publique on public.%I for select to anon, authenticated using (publie or public.is_admin())', t);
    execute format('drop policy if exists ecriture_admin on public.%I', t);
    execute format('create policy ecriture_admin on public.%I for all to authenticated using (public.is_admin()) with check (public.is_admin())', t);
  end loop;
  -- Données privées : admins uniquement
  foreach t in array array['prospects', 'demandes', 'historique', 'publications'] loop
    execute format('drop policy if exists admin_seul on public.%I', t);
    execute format('create policy admin_seul on public.%I for all to authenticated using (public.is_admin()) with check (public.is_admin())', t);
  end loop;
end $$;

drop policy if exists lecture_publique on public.reglages;
create policy lecture_publique on public.reglages for select to anon, authenticated using (true);
drop policy if exists ecriture_admin on public.reglages;
create policy ecriture_admin on public.reglages for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists admin_lecture on public.admins;
create policy admin_lecture on public.admins for select to authenticated using (public.is_admin());

-- ---------------------------------------------------------------------------
-- Stockage des fichiers
-- media : photos et PDF du site, lisibles par tous, modifiables par les admins
-- demandes : photos des prospects (lot 3), privées
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('media', 'media', true, 15728640, array['image/webp', 'image/jpeg', 'image/png', 'application/pdf'])
on conflict (id) do update set public = excluded.public, file_size_limit = excluded.file_size_limit, allowed_mime_types = excluded.allowed_mime_types;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('demandes', 'demandes', false, 15728640, array['image/webp', 'image/jpeg', 'image/png'])
on conflict (id) do update set public = excluded.public, file_size_limit = excluded.file_size_limit, allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists media_ecriture_admin on storage.objects;
create policy media_ecriture_admin on storage.objects for insert to authenticated
  with check (bucket_id = 'media' and public.is_admin());
drop policy if exists media_maj_admin on storage.objects;
create policy media_maj_admin on storage.objects for update to authenticated
  using (bucket_id = 'media' and public.is_admin());
drop policy if exists media_suppr_admin on storage.objects;
create policy media_suppr_admin on storage.objects for delete to authenticated
  using (bucket_id = 'media' and public.is_admin());
drop policy if exists demandes_admin on storage.objects;
create policy demandes_admin on storage.objects for all to authenticated
  using (bucket_id = 'demandes' and public.is_admin())
  with check (bucket_id = 'demandes' and public.is_admin());

-- Droits de base (Supabase les accorde déjà, rappelés ici pour être explicite)
grant usage on schema public to anon, authenticated;
grant select on public.reglages, public.prestations, public.produits, public.realisations, public.avis to anon;
grant select, insert, update, delete on all tables in schema public to authenticated;
grant execute on function public.is_admin() to anon, authenticated;
revoke execute on function public.purger_prospects() from anon, authenticated;
