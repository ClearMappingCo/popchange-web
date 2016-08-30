CREATE TABLE comparison_qualities (
       id serial PRIMARY KEY,
       ordering integer NOT NULL UNIQUE,
       title varchar (255) NOT NULL,
       description text,
       colour char (7) NOT NULL,
       created timestamp with time zone NOT NULL,
       modified timestamp with time zone
);

INSERT INTO comparison_qualities (ordering, title, colour, created) VALUES
       (1, 'OK', '#00CC00', now()),
       (2, 'Not Great', '#FFBF00', now()),
       (3, 'Unknown', '#FF0000', now());


CREATE TABLE attribs (
       id serial PRIMARY KEY,
       cenus_year smallint NOT NULL,
       title varchar (255) NOT NULL,
       description text,
       counts_table varchar (63) NOT NULL,
       counts_filename varchar (255),
       counts_path varchar (255),
       counts_lookup_filename varchar (255),
       counts_lookup_path varchar (255),
       counts_lookup_types_filename varchar (255),
       counts_lookup_types_path varchar (255),
       rates_table varchar (63),
       rates_filename varchar (255),
       rates_path varchar (255),
       created timestamp with time zone NOT NULL,
       modified timestamp with time zone
);


CREATE TABLE attrib_comparisons (
       src_attrib_id integer NOT NULL REFERENCES attribs (id) ON DELETE CASCADE,
       dst_attrib_id integer NOT NULL REFERENCES attribs (id) ON DELETE CASCADE,
       auto_generated boolean DEFAULT false NOT NULL,
       exclude boolean DEFAULT false NOT NULL,
       comparison_quality_id integer NOT NULL REFERENCES comparison_qualities (id) ON DELETE CASCADE,
       description text,
       created timestamp with time zone NOT NULL,
       modified timestamp with time zone,
       PRIMARY KEY (src_attrib_id, dst_attrib_id)
);


CREATE TABLE clip_areas (
       id serial PRIMARY KEY,
       title varchar (255) NOT NULL,
       shapefile_table varchar (63) NOT NULL,
       created timestamp with time zone NOT NULL,
       modified timestamp with time zone
);



/* Users */

CREATE TABLE user_sectors (
       id serial PRIMARY KEY,
       ordering integer NOT NULL UNIQUE,
       title varchar (255) NOT NULL UNIQUE,
       created timestamp with time zone NOT NULL,
       modified timestamp with time zone
);

INSERT INTO user_sectors (title, ordering, created) VALUES
       ('Academic study / research', 1, now()),
       ('Schools', 2, now()),
       ('Central government', 3, now()),
       ('Private sector', 4, now()),
       ('Third sector', 5, now()),
       ('Personal use', 6, now());
       

CREATE TABLE users (
       id serial PRIMARY KEY,
       user_sector_id integer NOT NULL REFERENCES user_sectors (id) ON DELETE RESTRICT,
       username varchar (255) NOT NULL UNIQUE CHECK (username NOT SIMILAR TO '%@%'),
       password_hash varchar (255) NOT NULL,
       fullname varchar (255) NOT NULL,
       email varchar (255) NOT NULL UNIQUE,
       mailing_list boolean DEFAULT false NOT NULL,
       active boolean DEFAULT false NOT NULL,
       created timestamp with time zone NOT NULL,
       modified timestamp with time zone
);


CREATE TABLE user_password_reset_tokens (
       user_id integer PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
       token char (8) NOT NULL UNIQUE,
       expires timestamp with time zone NOT NULL
);


/* Miscellaneous */

CREATE TABLE settings (
       const varchar (255) PRIMARY KEY,
       val varchar (255) NOT NULL,
       created timestamp with time zone NOT NULL,
       modified timestamp with time zone
);

INSERT INTO settings (const, val, created) VALUES
       ('SERVER_MONITORING_HASH', '6dc10e458046cfa05cbaee5826ccb2eb08691963b776c0f661e9cb266857ae19', now());


/* Logging */

CREATE TABLE log_user_logins (
       id serial PRIMARY KEY,
       user_id integer NOT NULL,
       remote_ip inet NOT NULL,
       created timestamp with time zone NOT NULL
);

CREATE TABLE log_attrib_comparisons (
       id serial PRIMARY KEY,
       user_id integer NOT NULL,
       src_attrib_id integer NOT NULL,
       src_attrib_census_year smallint NOT NULL,
       src_attrib_title varchar (255) NOT NULL,
       dst_attrib_id integer NOT NULL,
       dst_attrib_census_year smallint NOT NULL,
       dst_attrib_title varchar (255) NOT NULL,
       exclude_low_count_cells boolean NOT NULL,
       counts_table boolean DEFAULT TRUE NOT NULL,
       remote_ip inet NOT NULL,
       created timestamp with time zone NOT NULL
);
