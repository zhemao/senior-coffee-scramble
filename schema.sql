drop table if exists students;
drop table if exists invitations;

drop index if exists students_by_uni;
drop index if exists invitations_by_inviter;
drop index if exists invitations_by_invitee;
drop index if exists invitations_by_pair;

create table students (
    id serial primary key,
    name varchar(255),
    uni varchar(8) unique,
    confirmed boolean default false
);

create table invitations (
    id serial primary key,
    inviter varchar(8),
    invitee varchar(8),
    confirmed boolean default false,
    email_sent boolean default false,
    unique (inviter, invitee)
);

create index students_by_uni on students (uni);
create index invitations_by_inviter on invitations (inviter);
create index invitations_by_invitee on invitations (invitee);
create index invitations_by_pair on invitations (inviter, invitee);
