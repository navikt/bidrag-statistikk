-- Table: engangsbeløp

-- DROP TABLE engangsbeløp;

CREATE TABLE IF NOT EXISTS engangsbeløp
(
    engangsbeløpsid integer NOT NULL GENERATED BY DEFAULT AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),
    vedtaksid integer NOT NULL,
    type varchar(50) NOT NULL,
    sak varchar(20) NOT NULL,
    skyldner varchar(20) NOT NULL,
    kravhaver varchar(20) NOT NULL,
    mottaker varchar(20) NOT NULL,
    beløp float,
    valutakode varchar(10),
    resultatkode varchar(255) NOT NULL,
    innkreving varchar(20) NOT NULL,
    beslutning varchar(50) NOT NULL,
    omgjør_vedtak_id integer,
    referanse varchar(20),
    delytelse_id varchar(32),
    ekstern_referanse varchar(20),
    CONSTRAINT engangsbeløp_pkey PRIMARY KEY (engangsbeløpsid),
    CONSTRAINT engangsbeløp_fk_vedtaksid FOREIGN KEY (vedtaksid)
        REFERENCES vedtak (vedtaksid) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    UNIQUE (vedtaksid, referanse)
)

    TABLESPACE pg_default;

CREATE INDEX idx_engangsbeløp_1 ON engangsbeløp(vedtaksid);