alter session set container=PDBORCL;
create tablespace TBS_YTEX_TEST datafile 'C:/code/data/oradata/orcl/TBS_YTEX_TEST.dbf' size 1M autoextend on online;
create user ytex_test identified by ytex_test default tablespace TBS_YTEX_TEST;
grant connect, resource to ytex_test;
grant create materialized view to ytex_test;
grant create view to ytex_test;

