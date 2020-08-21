select * from public.użytkownicy_aktywni;

select * from public.użytkownicy;

select * from public.hasła;

-- Zahaszowane hasła
select public.użytkownicy.nazwa, encode(public.hasła.hash, 'escape') as hash, encode(public.hasła.sól, 'escape') as sól 
from public.hasła
join public.użytkownicy
on public.hasła.user_idu=public.użytkownicy.idu;

-- Aktywni użytkownicy
select public.użytkownicy.nazwa, public.użytkownicy.punkty, public.użytkownicy_aktywni.rysuje
from public.użytkownicy_aktywni 
join public.użytkownicy
on public.użytkownicy_aktywni.idu=public.użytkownicy.idu;
