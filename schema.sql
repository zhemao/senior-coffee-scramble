drop table if exists students;
drop table if exists invitations;

drop index if exists invitations_by_inviter;
drop index if exists invitations_by_invitee;

create table students (
    id serial primary key,
    name varchar(64),
    uni varchar(7) unique,
    confirmed boolean default false
);

create table invitations (
    id serial primary key,
    inviter varchar(7),
    invitee varchar(7),
    message varchar(140),
    confirmed boolean default false,
    email_sent boolean default false,
    unique (inviter, invitee)
);

create index invitations_by_inviter on invitations (inviter);
create index invitations_by_invitee on invitations (invitee);
