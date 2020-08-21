select * from public.użytkownicy_aktywni;

select * from public.użytkownicy;

select * from public.hasła;

select encode(public.hasła.hash, 'escape') as hash, encode(public.hasła.sól, 'escape') as sól 
from public.hasła;
