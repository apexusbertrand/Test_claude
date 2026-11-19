-- Tests des droits d'accès. Lancer après stub-supabase.sql, schema.sql et seed.sql.
-- Chaque test lève une erreur s'il échoue.
\set ON_ERROR_STOP 1

insert into auth.users values ('11111111-1111-1111-1111-111111111111', 'nataliia@example.test'),
                              ('22222222-2222-2222-2222-222222222222', 'inconnu@example.test');
insert into public.admins values ('11111111-1111-1111-1111-111111111111', 'nataliia@example.test');

-- Brouillon et donnée privée, créés par le système
insert into public.realisations (slug, titre, type, apres, publie) values ('brouillon', 'Brouillon', 'terrasses', '[{"path":"a.webp"}]', false);
insert into public.realisations (slug, titre, type, apres, publie) values ('publiee', 'Publiée', 'terrasses', '[{"path":"b.webp"}]', true);
insert into public.prospects (nom, email, telephone) values ('Client', 'client@example.test', '0600000000');

-- 1. Visiteur anonyme : voit les contenus publiés, rien d'autre
set role anon;
select set_config('request.jwt.claim.sub', '', false);
do $$ begin
  if (select count(*) from public.realisations) <> 1 then raise exception 'anon voit des brouillons'; end if;
  if (select count(*) from public.prestations) <> 8 then raise exception 'anon ne voit pas les prestations'; end if;
  if (select count(*) from public.reglages) <> 1 then raise exception 'anon ne voit pas les réglages'; end if;
end $$;
do $$ begin
  perform * from public.prospects;
  raise exception 'anon lit les prospects';
exception when insufficient_privilege then null;
end $$;
do $$ begin
  insert into public.avis (nom, texte) values ('x', 'y');
  raise exception 'anon peut écrire';
exception when insufficient_privilege then null;
end $$;
reset role;

-- 2. Utilisateur connecté mais pas admin : comme un visiteur, et ne peut rien écrire
set role authenticated;
select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', false);
do $$ begin
  if (select count(*) from public.realisations) <> 1 then raise exception 'non-admin voit des brouillons'; end if;
  if (select count(*) from public.prospects) <> 0 then raise exception 'non-admin voit les prospects'; end if;
end $$;
do $$ begin
  insert into public.avis (nom, texte) values ('x', 'y');
  raise exception 'non-admin peut écrire';
exception when insufficient_privilege then null;
end $$;
do $$ begin
  insert into storage.objects (bucket_id, name) values ('media', 'x.webp');
  raise exception 'non-admin peut envoyer un fichier';
exception when insufficient_privilege then null;
end $$;
reset role;

-- 3. Admin : tout, et chaque modification est journalisée avec son e-mail
set role authenticated;
select set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', false);
do $$ begin
  if (select count(*) from public.realisations) <> 2 then raise exception 'admin ne voit pas les brouillons'; end if;
  if (select count(*) from public.prospects) <> 1 then raise exception 'admin ne voit pas les prospects'; end if;
end $$;
insert into public.avis (nom, texte) values ('Mme D.', 'Très belle terrasse.');
update public.produits set nom = 'Granulat clair modifié' where slug = 'exemple-granulat-clair';
insert into storage.objects (bucket_id, name) values ('media', 'realisations/test.webp');
do $$ begin
  if (select auteur from public.historique where table_name = 'produits' and action = 'UPDATE' order by id desc limit 1) <> 'nataliia@example.test'
    then raise exception 'historique sans auteur'; end if;
  if (select avant->>'nom' from public.historique where table_name = 'produits' and action = 'UPDATE' order by id desc limit 1) <> 'Granulat clair (exemple)'
    then raise exception 'historique sans valeur précédente'; end if;
end $$;
-- Contraintes : réalisation sans photo après, ou avec 3 photos avant
do $$ begin
  insert into public.realisations (slug, titre, type, apres) values ('vide', 'Vide', 'terrasses', '[]');
  raise exception 'réalisation sans photo acceptée';
exception when check_violation then null;
end $$;
do $$ begin
  insert into public.realisations (slug, titre, type, apres, avant) values ('trop', 'Trop', 'terrasses', '[{"path":"a"}]', '[{},{},{}]');
  raise exception '3 photos avant acceptées';
exception when check_violation then null;
end $$;
reset role;

-- E-mail en majuscules refusé
do $$ begin
  insert into public.prospects (nom, email, telephone) values ('X', 'Majuscules@Test.fr', '0600000000');
  raise exception 'e-mail en majuscules accepté';
exception when check_violation then null;
end $$;

-- 4. Purge RGPD
update public.prospects set dernier_contact = now() - interval '3 years 1 day';
do $$ begin
  if public.purger_prospects() <> 1 then raise exception 'purge RGPD inopérante'; end if;
end $$;

select 'Tous les tests de droits passent' as resultat;
