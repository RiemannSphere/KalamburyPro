select * from public.użytkownicy_aktywni;

select * from public.użytkownicy;

select * from public.hasła;

select * from public.słownik_aplikacji;

select * from public.slowa;

-- Usuń tabele poza wymaganymi
drop table public.użytkownicy_aktywni;
drop table public.hasła;
drop table public.użytkownicy;
drop table public.sequence;

-- Usuwanie danych
delete from public.użytkownicy_aktywni 
where idau='2';

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

-- Uzupełnij słownik o podstawowe dane aplikacji
insert into public.słownik_aplikacji (klucz, wartość)
values ('SECRET', '2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b');

insert into public.słownik_aplikacji (klucz, wartość)
values ('OWNERS', 'Piotr Kołodziejski, Maciej Szabała');

insert into public.słownik_aplikacji (klucz, wartość)
values ('EXP_TIME_MILLIS', '86400000');

insert into public.słownik_aplikacji (klucz, wartość)
values ('NUM_OF_RETRY', '5');

insert into public.słownik_aplikacji (klucz, wartość)
values ('RETRY_FREQ_IN_MILLIS', '1000');