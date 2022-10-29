create table users (
    id int auto_increment primary key,
    user_id varchar(20),
    pwd varchar(20),
    name varchar(20),
    created_at datetime default NOW()
);