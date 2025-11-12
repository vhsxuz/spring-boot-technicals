--
-- PostgreSQL database dump
--

-- Dumped from database version 17.6 (Homebrew)
-- Dumped by pg_dump version 17.5

-- Started on 2025-11-12 20:47:18 WIB

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 108950)
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- TOC entry 3948 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- TOC entry 266 (class 1255 OID 109137)
-- Name: grant_viewer_on_user_insert(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.grant_viewer_on_user_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  v_role_id uuid;
BEGIN
  SELECT id INTO v_role_id FROM public.roles WHERE role_key = 'VIEWER';
  IF v_role_id IS NOT NULL THEN
    INSERT INTO public.user_roles (user_id, role_id)
    VALUES (NEW.id, v_role_id)
    ON CONFLICT DO NOTHING;
  END IF;
  RETURN NEW;
END $$;


ALTER FUNCTION public.grant_viewer_on_user_insert() OWNER TO postgres;

--
-- TOC entry 265 (class 1255 OID 109131)
-- Name: has_permission(uuid, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.has_permission(p_user_id uuid, p_permission_key text) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.user_roles ur
    JOIN public.role_permissions rp ON rp.role_id = ur.role_id
    JOIN public.permissions perm ON perm.id = rp.permission_id
    WHERE ur.user_id = p_user_id
      AND perm.permission_key = p_permission_key
  );
$$;


ALTER FUNCTION public.has_permission(p_user_id uuid, p_permission_key text) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 220 (class 1259 OID 109012)
-- Name: articles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.articles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title text NOT NULL,
    content text NOT NULL,
    author_id uuid NOT NULL,
    is_public boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.articles OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 109028)
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid,
    action text NOT NULL,
    entity_type text NOT NULL,
    entity_id text,
    method text,
    path text,
    ip_address character varying(255),
    user_agent text,
    details jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.audit_logs OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 109042)
-- Name: auth_otps; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.auth_otps (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    code character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    consumed_at timestamp with time zone
);


ALTER TABLE public.auth_otps OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 109106)
-- Name: permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    permission_key text NOT NULL,
    description text
);


ALTER TABLE public.permissions OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 109116)
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);


ALTER TABLE public.role_permissions OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 109002)
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    role_key text NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 109070)
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    granted_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 108987)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    fullname text NOT NULL,
    username text NOT NULL,
    email text NOT NULL,
    password_hash text NOT NULL,
    is_email_verified boolean DEFAULT false NOT NULL,
    blocked_until timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 109132)
-- Name: effective_permissions_by_user; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.effective_permissions_by_user AS
 SELECT u.id AS user_id,
    u.email,
    r.role_key,
    perm.permission_key
   FROM ((((public.users u
     JOIN public.user_roles ur ON ((ur.user_id = u.id)))
     JOIN public.roles r ON ((r.id = ur.role_id)))
     JOIN public.role_permissions rp ON ((rp.role_id = r.id)))
     JOIN public.permissions perm ON ((perm.id = rp.permission_id)));


ALTER VIEW public.effective_permissions_by_user OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 109057)
-- Name: login_attempts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.login_attempts (
    id bigint NOT NULL,
    user_id uuid,
    identifier text NOT NULL,
    success boolean NOT NULL,
    reason text,
    ip_address character varying(255),
    user_agent text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.login_attempts OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 109056)
-- Name: login_attempts_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.login_attempts ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.login_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 3935 (class 0 OID 109012)
-- Dependencies: 220
-- Data for Name: articles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.articles (id, title, content, author_id, is_public, created_at, updated_at) FROM stdin;
9ecfd457-0fe5-4f54-bdb4-99ca697e96bb	Hello World with Roles	This is a public welcome article.	c74494f2-b34e-4404-9cac-c47966db18b0	t	2025-11-11 21:53:53.666099+07	2025-11-11 21:53:53.666099+07
d9204240-fcbd-4d39-a442-de00d87801b0	Private Draft: Roadmap Q1	Early ideas and tasks. Work-in-progress.	c74494f2-b34e-4404-9cac-c47966db18b0	f	2025-11-11 21:53:53.666099+07	2025-11-11 21:53:53.666099+07
6fd4149c-ef94-461f-9e92-5efdda749559	Viewer Notes: Getting Started	Notes from a new user perspective.	e7100b5a-6394-46f9-a3cd-44753483156c	t	2025-11-11 21:53:53.666099+07	2025-11-11 21:53:53.666099+07
\.


--
-- TOC entry 3936 (class 0 OID 109028)
-- Dependencies: 221
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_logs (id, actor_user_id, action, entity_type, entity_id, method, path, ip_address, user_agent, details, created_at) FROM stdin;
600d77f9-f603-493d-8077-54cc7f92adcc	\N	AUTH_LOGIN_FAILED	user	\N	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	curl/8.7.1	{"reason": "user_not_found"}	2025-11-11 20:53:42.2607+07
fab0f6d7-abfa-4668-bf59-ce153d36cb65	7ea24032-7c82-4ccf-b530-7e997b82145e	CREATE	article	9ecfd457-0fe5-4f54-bdb4-99ca697e96bb	POST	/api/articles	192.168.1.10/32	Mozilla/5.0	{"note": "Seeded creation log"}	2025-11-11 21:53:53.666099+07
59ece3db-fe7d-4384-9f05-10d8144d69a6	\N	MFA_OTP_ISSUED	user	64bd1961-fc25-4f0c-85cb-98cb6ce40747	\N	\N	\N	\N	{"otpId": "f33e13f0-3476-4234-bd4e-47e31ff644db"}	2025-11-11 22:01:20.205995+07
1acedcbb-d378-451a-95ce-df27558166be	\N	AUTH_REGISTER	user	64bd1961-fc25-4f0c-85cb-98cb6ce40747	POST	/api/v1/auth/register	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"email": "drelexander.dev@gmail.com", "username": "andreas"}	2025-11-11 22:01:20.208637+07
9bafb3c4-52ee-48c8-b445-ce777765b9f8	\N	MFA_OTP_ISSUED	user	74f48136-5c23-40b2-9743-b05aea8a3ea2	\N	\N	\N	\N	{"otpId": "355f741d-ad61-4fa0-88aa-62f70538bcb3"}	2025-11-11 22:05:03.003001+07
6f9619e0-ffec-497a-b4d5-edf9a99c927f	\N	AUTH_REGISTER	user	74f48136-5c23-40b2-9743-b05aea8a3ea2	POST	/api/v1/auth/register	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"email": "drealexander.dev@gmail.com", "username": "andreas"}	2025-11-11 22:05:03.003326+07
e6e44896-cdb6-43c6-bd90-50f2d5ca7dd6	\N	MFA_OTP_ISSUED	user	35eaeb37-11a8-44c3-a24c-583882075083	\N	\N	\N	\N	{"otpId": "774a1fc5-5423-4fa7-b44e-c64d123f002c"}	2025-11-11 22:17:32.484691+07
ef2b26d5-c9f0-4b56-bb4f-4c3c1e39060a	\N	AUTH_REGISTER	user	35eaeb37-11a8-44c3-a24c-583882075083	POST	/api/v1/auth/register	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"email": "drealexander.dev@gmail.com", "username": "andreas"}	2025-11-11 22:17:32.487495+07
91cdecd1-ddb5-4d1b-af62-4a90455d841c	\N	MFA_OTP_ISSUED	user	9c2a6129-d2dc-4a1a-a630-904c7864e230	\N	\N	\N	\N	{"otpId": "73f98553-8bbc-466f-ac39-e63744e7c5e5"}	2025-11-11 22:25:31.369384+07
ba90bb16-3d38-4217-be0b-bbf502632c3c	\N	AUTH_REGISTER	user	9c2a6129-d2dc-4a1a-a630-904c7864e230	POST	/api/v1/auth/register	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"email": "drealexander.dev@gmail.com", "username": "andreas"}	2025-11-11 22:25:31.386623+07
7be578fc-2924-48aa-9e73-3cc03667e95d	\N	MFA_OTP_ISSUED	user	a0c68edc-bc2b-49b8-919b-a16dc8d0632c	\N	\N	\N	\N	{"otpId": "3ec0367f-b305-4018-bcfe-9d7039d62204"}	2025-11-11 22:31:53.334672+07
460d9c8a-7a79-4a4e-b80a-839b94f4152a	\N	AUTH_REGISTER	user	a0c68edc-bc2b-49b8-919b-a16dc8d0632c	POST	/api/v1/auth/register	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"email": "drealexander.dev@gmail.com", "username": "andreases"}	2025-11-11 22:31:53.344635+07
afddf3e1-630b-4252-bd87-9266f03d596c	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "278b2517-6116-4829-aafc-c4a1bef3e563"}	2025-11-11 22:50:16.10328+07
f1581683-308e-450e-b852-9ea32a164790	\N	AUTH_REGISTER	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/register	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"email": "drealexander.dev@gmail.com", "username": "andreas3"}	2025-11-11 22:50:16.116309+07
deae45f0-0fa0-401c-a2e3-329fc2aa0815	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["VIEWER"]}	2025-11-11 22:57:01.34262+07
797890f9-b4e5-4088-a639-fefeaa7f1482	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "414a7833-e92d-4682-b775-d83abc41ae8c"}	2025-11-11 23:17:32.102434+07
ed2bd939-c361-46e1-8980-41887f0f3cee	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "414a7833-e92d-4682-b775-d83abc41ae8c"}	2025-11-11 23:17:32.266418+07
9b48b290-11b9-4f7d-bd4d-7d370435e7c3	\N	MFA_OTP_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "invalid_or_expired"}	2025-11-11 23:18:09.773047+07
3617cbef-f056-4901-a822-e2652efe0b93	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["VIEWER"]}	2025-11-11 23:19:04.805417+07
eedcc60b-2ada-4476-a73d-8bdf43935404	\N	USER_LIST	user	\N	GET	/api/v1/users	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"limit": 20, "offset": 0, "returned": 6, "emailLike": "null", "usernameLike": "null"}	2025-11-11 23:19:34.558633+07
4728b799-53ce-46e5-ae69-57dc9d98a69a	\N	READ	User	\N	GET	/api/v1/users	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"limit": 20, "offset": 0, "returned": 6}	2025-11-11 23:19:34.577112+07
b3339304-8510-4375-9329-6a6a46f82fc9	\N	AUTH_LOGIN_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "bad_password"}	2025-11-11 23:20:12.80959+07
54bbb80b-008c-4ec4-866f-15f23d95fdc9	\N	AUTH_LOGIN_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "bad_password"}	2025-11-11 23:20:15.316887+07
bef6a113-e911-4691-8f96-8ebd73417652	\N	AUTH_LOGIN_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "bad_password"}	2025-11-11 23:20:16.583054+07
e126d1c0-d188-48d5-a6b4-583fca315570	\N	AUTH_LOGIN_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "bad_password"}	2025-11-11 23:20:17.793332+07
480114f5-6cd3-423c-8530-424b044b16fa	\N	AUTH_LOGIN_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "bad_password"}	2025-11-11 23:20:18.933616+07
48884eb0-4a5d-45ad-9034-1fafe0ac7629	\N	AUTH_LOGIN_BLOCKED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"blockedUntil": "2025-11-11T16:50:18.921505Z"}	2025-11-11 23:20:20.474723+07
3d51b777-4018-4051-a7bd-5de84d85f600	e7100b5a-6394-46f9-a3cd-44753483156c	READ	Article	6fd4149c-ef94-461f-9e92-5efdda749559	GET	/api/v1/articles/6fd4149c-ef94-461f-9e92-5efdda749559	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"hit": true, "actorId": "null"}	2025-11-11 23:52:59.0987+07
4c739299-85e6-4226-b0cf-5ca96b02cabe	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "cfdb7bba-0054-459f-9011-28594c2dff8d"}	2025-11-12 00:01:03.501507+07
6b5dde66-33dc-43e2-a2ae-88070f879961	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "cfdb7bba-0054-459f-9011-28594c2dff8d"}	2025-11-12 00:01:03.568503+07
a683471b-f305-439c-9e81-cbd2218f572b	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["VIEWER"]}	2025-11-12 00:02:48.316436+07
bc024dc1-8ca5-41ee-8db4-9227708fa028	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "18f90791-ecf4-40c4-bc2c-6d34ba604a43"}	2025-11-12 00:26:43.008873+07
4521ab3e-ef38-468a-bf62-4828da23a1ec	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "18f90791-ecf4-40c4-bc2c-6d34ba604a43"}	2025-11-12 00:26:43.063717+07
976ee384-553d-4d65-8f9b-a71b39a6c1d7	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["CONTRIBUTOR"]}	2025-11-12 00:27:43.70513+07
3d19fc2c-da5f-412b-b57a-0fcffc0623c8	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "ce48fce6-30cb-46ce-a6e4-f6acab2b0d51"}	2025-11-12 05:08:58.323887+07
2198412e-6c7e-4d7b-b732-422bd004bfad	1d8b9182-d25d-4150-ab78-1110a569adb0	LIST	Article	\N	GET	/api/v1/articles	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"count": 2, "limit": 20, "offset": 0}	2025-11-12 06:50:25.562275+07
ea626b88-b6b0-4079-8885-77d2e6d9d9a2	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "ce48fce6-30cb-46ce-a6e4-f6acab2b0d51"}	2025-11-12 05:08:58.360268+07
421a6539-fe4d-4210-b95c-2f691a162753	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["VIEWER"]}	2025-11-12 05:09:23.474555+07
04805160-0a4e-40e5-be50-6d357d93e489	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "7a0b5d29-7c17-4262-b27d-bc063b0ca575"}	2025-11-12 06:14:52.68384+07
e7e8dff9-cfe3-4c19-8e2b-58b3ab70acac	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "7a0b5d29-7c17-4262-b27d-bc063b0ca575"}	2025-11-12 06:14:52.729923+07
a5352abf-1383-45cc-84d7-3d7f8b43c802	\N	MFA_OTP_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "wrong_code"}	2025-11-12 06:15:04.626903+07
b3dd2d40-010a-4314-8e55-58f36e00ab9a	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["CONTRIBUTOR"]}	2025-11-12 06:15:20.065225+07
724a38d1-4a31-46c1-bd13-9c34779d1b82	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "588fb357-9154-497c-8d3a-0012fc0d07bb"}	2025-11-12 06:17:51.255045+07
7690b782-b081-4242-8cd7-67a21ad22727	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "588fb357-9154-497c-8d3a-0012fc0d07bb"}	2025-11-12 06:17:51.29795+07
9bdd4325-1d0f-43b9-b4c5-660d50489f4b	\N	MFA_OTP_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "wrong_code"}	2025-11-12 06:18:09.644902+07
81428e98-59e3-495f-aee8-4c4e84cec96c	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": []}	2025-11-12 06:18:21.095027+07
c5920569-2e97-4d91-9ddf-5d76ecdc8901	\N	MFA_OTP_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "invalid_or_expired"}	2025-11-12 06:21:49.445712+07
68f63552-8e70-47b0-a452-97e5ea490a63	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "bc347b06-fa97-4706-9a14-98cbb6cef457"}	2025-11-12 06:21:56.784575+07
9e623679-475e-41a5-94b0-2c58fecef184	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "bc347b06-fa97-4706-9a14-98cbb6cef457"}	2025-11-12 06:21:56.820547+07
06774037-3f6e-4eae-bf52-2c01af13b968	\N	MFA_OTP_FAILED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "wrong_code"}	2025-11-12 06:22:04.411813+07
f982e4c3-05d9-4e02-ae29-ebb8aae9a64e	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["CONTRIBUTOR"]}	2025-11-12 06:22:22.241793+07
ba1d560a-0754-4cf4-b773-6411b97e83ce	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "9fd3108f-0c22-4f50-8f02-53e2e75d0e82"}	2025-11-12 06:30:16.400821+07
b5a2ce7c-9218-4f8d-bf56-a26141956c6f	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "9fd3108f-0c22-4f50-8f02-53e2e75d0e82"}	2025-11-12 06:30:16.618404+07
52650ded-5b09-42ea-94be-c607511347ae	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["CONTRIBUTOR"]}	2025-11-12 06:30:50.444287+07
880bf37a-5283-4381-9ab7-c54c8d9d647f	35a1162e-5d35-414c-bd0b-71c050667ebd	CREATE	Article	8fea359d-7107-42a5-a29e-6e8da707dbb5	POST	/api/v1/articles	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"title": "Spring Tips", "isPublic": true}	2025-11-12 06:31:14.914763+07
c9a8543c-e4ec-4b2e-a818-cfc289c65608	35a1162e-5d35-414c-bd0b-71c050667ebd	UPDATE	Article	8fea359d-7107-42a5-a29e-6e8da707dbb5	PATCH	/api/v1/articles/8fea359d-7107-42a5-a29e-6e8da707dbb5	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"title": "Spring Tips Eps 2", "isPublic": true}	2025-11-12 06:32:57.311289+07
e26400c8-2854-4a57-bd86-a21bf93c28c6	35a1162e-5d35-414c-bd0b-71c050667ebd	UPDATE	Article	8fea359d-7107-42a5-a29e-6e8da707dbb5	PATCH	/api/v1/articles/8fea359d-7107-42a5-a29e-6e8da707dbb5	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"title": "Spring Tips Eps 3", "isPublic": true}	2025-11-12 06:34:15.188217+07
152f4c81-c817-43a0-9a24-c1c8b37cd68a	\N	MFA_OTP_ISSUED	user	35a1162e-5d35-414c-bd0b-71c050667ebd	\N	\N	\N	\N	{"otpId": "05a03247-0ea2-4426-8590-ae95a7209812"}	2025-11-12 06:40:02.651045+07
7fc90143-15a7-437a-84e6-0c7280e6eb27	\N	AUTH_LOGIN_PASSWORD_OK	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/login	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"otpId": "05a03247-0ea2-4426-8590-ae95a7209812"}	2025-11-12 06:40:02.853844+07
9e686c6e-5b5a-409f-9591-be5f7cae2a70	\N	AUTH_LOGIN_SUCCESS	user	35a1162e-5d35-414c-bd0b-71c050667ebd	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["EDITOR"]}	2025-11-12 06:40:27.347878+07
950b461c-411a-4392-a60e-7fef6c483751	35a1162e-5d35-414c-bd0b-71c050667ebd	DELETE	Article	8fea359d-7107-42a5-a29e-6e8da707dbb5	DELETE	/api/v1/articles/8fea359d-7107-42a5-a29e-6e8da707dbb5	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"deleted": true}	2025-11-12 06:40:43.963829+07
1dbcc2d4-26a0-4cd7-9984-310398837d01	35a1162e-5d35-414c-bd0b-71c050667ebd	CREATE	Article	71404e75-787d-40c3-9f9c-d5e81fccb2f2	POST	/api/v1/articles	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"title": "Spring Tips", "isPublic": true}	2025-11-12 06:41:16.099524+07
8125bc2a-67e4-4579-b312-2f40504e4ba1	35a1162e-5d35-414c-bd0b-71c050667ebd	UPDATE	Article	71404e75-787d-40c3-9f9c-d5e81fccb2f2	PATCH	/api/v1/articles/71404e75-787d-40c3-9f9c-d5e81fccb2f2	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"title": "Spring Tips Eps 3", "isPublic": true}	2025-11-12 06:41:39.262433+07
b114b7e0-da56-45a0-b5bd-fa232b43f873	35a1162e-5d35-414c-bd0b-71c050667ebd	DELETE	Article	71404e75-787d-40c3-9f9c-d5e81fccb2f2	DELETE	/api/v1/articles/71404e75-787d-40c3-9f9c-d5e81fccb2f2	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"deleted": true}	2025-11-12 06:41:45.092695+07
186fd0f2-08ba-464c-b083-d684bd54a032	\N	USER_LIST	user	\N	GET	/api/v1/users	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"limit": 20, "offset": 0, "returned": 6, "emailLike": "null", "usernameLike": "null"}	2025-11-12 06:42:34.594707+07
0afaa1ea-aae7-45e4-9ba9-e901e11e987c	35a1162e-5d35-414c-bd0b-71c050667ebd	READ	User	\N	GET	/api/v1/users	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"limit": 20, "offset": 0, "returned": 6}	2025-11-12 06:42:34.608873+07
8c62cc47-77cd-4184-b509-8f56186c486b	\N	MFA_OTP_ISSUED	user	1d8b9182-d25d-4150-ab78-1110a569adb0	\N	\N	\N	\N	{"otpId": "e04f09bd-f458-4d18-befa-92bc3f1571bb"}	2025-11-12 06:47:46.413595+07
17fff11a-e238-4e3d-8d10-c39bbc25d537	\N	AUTH_REGISTER	user	1d8b9182-d25d-4150-ab78-1110a569adb0	POST	/api/v1/auth/register	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"email": "andreas.alexander991@gmail.com", "username": "andreas4"}	2025-11-12 06:47:46.429795+07
58325040-940c-49b9-883b-48462f41db04	\N	MFA_OTP_FAILED	user	1d8b9182-d25d-4150-ab78-1110a569adb0	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"reason": "wrong_code"}	2025-11-12 06:49:43.131192+07
935eb7a3-b7fd-46fc-ab99-37f2713b0004	\N	AUTH_LOGIN_SUCCESS	user	1d8b9182-d25d-4150-ab78-1110a569adb0	POST	/api/v1/auth/verify-otp	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"roles": ["VIEWER"]}	2025-11-12 06:50:04.788965+07
6a611565-9b4a-47c5-b82a-47bdf5fde7ea	\N	USER_LIST	user	\N	GET	/api/v1/users	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"limit": 20, "offset": 0, "returned": 7, "emailLike": "null", "usernameLike": "null"}	2025-11-12 06:50:09.558932+07
e393a646-26ea-4eb7-a47c-57156d3d87c9	35a1162e-5d35-414c-bd0b-71c050667ebd	LIST	User	\N	GET	/api/v1/users	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"limit": 20, "offset": 0, "returned": 7}	2025-11-12 06:50:09.582045+07
45da4134-b069-4cac-b695-ddfb18fec3b6	1d8b9182-d25d-4150-ab78-1110a569adb0	LIST	Article	\N	GET	/api/v1/articles	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"count": 2, "limit": 20, "offset": 0}	2025-11-12 06:51:58.364481+07
f5901254-de3a-44b2-9318-4539efa6c445	1d8b9182-d25d-4150-ab78-1110a569adb0	LIST	Article	\N	GET	/api/v1/articles	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	{"count": 2, "limit": 20, "offset": 0}	2025-11-12 06:53:52.493947+07
\.


--
-- TOC entry 3937 (class 0 OID 109042)
-- Dependencies: 222
-- Data for Name: auth_otps; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.auth_otps (id, user_id, code, created_at, expires_at, consumed_at) FROM stdin;
f38a8b1f-b79a-4409-9397-8dd06df0a5e9	c74494f2-b34e-4404-9cac-c47966db18b0	123456	2025-11-11 21:53:53.666099+07	2025-11-11 22:03:53.666099+07	\N
79250e61-9f4c-4ddb-816c-fd93481f1118	e7100b5a-6394-46f9-a3cd-44753483156c	654321	2025-11-11 20:53:53.666099+07	2025-11-11 21:03:53.666099+07	2025-11-11 20:58:53.666099+07
73f98553-8bbc-466f-ac39-e63744e7c5e5	9c2a6129-d2dc-4a1a-a630-904c7864e230	981396	2025-11-11 22:25:29.243898+07	2025-11-11 22:35:29.243905+07	\N
3ec0367f-b305-4018-bcfe-9d7039d62204	a0c68edc-bc2b-49b8-919b-a16dc8d0632c	426850	2025-11-11 22:31:51.315559+07	2025-11-11 22:41:51.315565+07	\N
278b2517-6116-4829-aafc-c4a1bef3e563	35a1162e-5d35-414c-bd0b-71c050667ebd	512865	2025-11-11 22:50:12.669177+07	2025-11-11 23:00:12.669185+07	2025-11-11 22:57:01.23919+07
414a7833-e92d-4682-b775-d83abc41ae8c	35a1162e-5d35-414c-bd0b-71c050667ebd	964191	2025-11-11 23:17:29.090261+07	2025-11-11 23:27:29.090272+07	2025-11-11 23:19:04.707482+07
cfdb7bba-0054-459f-9011-28594c2dff8d	35a1162e-5d35-414c-bd0b-71c050667ebd	048652	2025-11-12 00:00:59.84639+07	2025-11-12 00:10:59.846404+07	2025-11-12 00:02:48.250687+07
18f90791-ecf4-40c4-bc2c-6d34ba604a43	35a1162e-5d35-414c-bd0b-71c050667ebd	175244	2025-11-12 00:26:39.601487+07	2025-11-12 00:36:39.6015+07	2025-11-12 00:27:43.67625+07
ce48fce6-30cb-46ce-a6e4-f6acab2b0d51	35a1162e-5d35-414c-bd0b-71c050667ebd	393974	2025-11-12 05:08:54.814751+07	2025-11-12 05:18:54.814765+07	2025-11-12 05:09:23.41402+07
7a0b5d29-7c17-4262-b27d-bc063b0ca575	35a1162e-5d35-414c-bd0b-71c050667ebd	123929	2025-11-12 06:14:49.265249+07	2025-11-12 06:24:49.265264+07	2025-11-12 06:15:19.983903+07
588fb357-9154-497c-8d3a-0012fc0d07bb	35a1162e-5d35-414c-bd0b-71c050667ebd	518727	2025-11-12 06:17:48.407185+07	2025-11-12 06:27:48.407196+07	2025-11-12 06:18:21.060316+07
bc347b06-fa97-4706-9a14-98cbb6cef457	35a1162e-5d35-414c-bd0b-71c050667ebd	282997	2025-11-12 06:21:53.639787+07	2025-11-12 06:31:53.639797+07	2025-11-12 06:22:22.19431+07
9fd3108f-0c22-4f50-8f02-53e2e75d0e82	35a1162e-5d35-414c-bd0b-71c050667ebd	085871	2025-11-12 06:30:12.952251+07	2025-11-12 06:40:12.952266+07	2025-11-12 06:30:50.412834+07
05a03247-0ea2-4426-8590-ae95a7209812	35a1162e-5d35-414c-bd0b-71c050667ebd	977082	2025-11-12 06:39:57.970237+07	2025-11-12 06:49:57.970251+07	2025-11-12 06:40:27.310732+07
e04f09bd-f458-4d18-befa-92bc3f1571bb	1d8b9182-d25d-4150-ab78-1110a569adb0	719854	2025-11-12 06:47:42.396876+07	2025-11-12 06:57:42.396886+07	2025-11-12 06:50:04.724538+07
\.


--
-- TOC entry 3939 (class 0 OID 109057)
-- Dependencies: 224
-- Data for Name: login_attempts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.login_attempts (id, user_id, identifier, success, reason, ip_address, user_agent, created_at) FROM stdin;
1	c74494f2-b34e-4404-9cac-c47966db18b0	alice@example.com	t	\N	203.0.113.7/32	Chrome/122	2025-11-11 21:53:53.666099+07
2	\N	unknown@example.com	f	email_not_found	203.0.113.9/32	Safari/17	2025-11-11 21:53:53.666099+07
3	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-11 23:17:32.212111+07
4	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	f	bad_password	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-11 23:20:12.672299+07
5	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	f	bad_password	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-11 23:20:15.313185+07
6	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	f	bad_password	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-11 23:20:16.578731+07
7	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	f	bad_password	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-11 23:20:17.790791+07
8	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	f	bad_password	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-11 23:20:18.917146+07
9	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	f	blocked	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-11 23:20:20.470037+07
10	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 00:01:03.56036+07
11	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 00:26:43.055339+07
12	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 05:08:58.352445+07
13	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 06:14:52.719003+07
14	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 06:17:51.286592+07
15	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 06:21:56.805868+07
16	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 06:30:16.559951+07
17	35a1162e-5d35-414c-bd0b-71c050667ebd	drealexander.dev@gmail.com	t	password_ok_mfa_pending	0:0:0:0:0:0:0:1	PostmanRuntime/7.50.0	2025-11-12 06:40:02.803183+07
\.


--
-- TOC entry 3941 (class 0 OID 109106)
-- Dependencies: 226
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.permissions (id, permission_key, description) FROM stdin;
f313be33-f79d-440a-9226-617566fafd63	users.create	Create users
580158b0-2732-4a44-a737-c7a91f54d74a	users.read	Read users
a75b253c-da4f-4000-9e70-52805cfbc156	users.update	Update users
1527b444-22b9-4c88-ab8e-a5c88d1fd7f1	users.delete	Delete users
9990d4df-dd02-47e2-be96-17d0f85e66c0	articles.create_any	Create article for any author
e81b4811-39bf-47dd-8d2d-1cc5181aeb7c	articles.read_any	Read all articles (public and private)
2ce9dfec-12ee-469b-9a6b-2d9a07d9991d	articles.update_any	Update any article
ff3ed73c-ea99-45ef-8aef-6b7b2d217b6e	articles.delete_any	Delete any article
bae712b3-49ce-42ce-8e7a-b795ae100ae4	articles.create_own	Create article for self
62ed7d0b-2c6e-4203-b3bd-c07ab9fd7c71	articles.read_own	Read own articles
414cd3a3-8035-4be5-9f07-550c12e9cd65	articles.update_own	Update own articles
f6596d7b-4e5c-4c57-ab0a-dcf8970885c3	articles.delete_own	Delete own articles
c07ba9b0-acc1-46e9-afcf-b521279b7a92	articles.read_public	Read only public articles
e4506300-f468-437f-a113-f457757d9767	audit_logs.read_all	Read all audit logs
\.


--
-- TOC entry 3942 (class 0 OID 109116)
-- Dependencies: 227
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.role_permissions (role_id, permission_id) FROM stdin;
c983baec-2781-4580-85d6-664d39ad5011	f313be33-f79d-440a-9226-617566fafd63
c983baec-2781-4580-85d6-664d39ad5011	580158b0-2732-4a44-a737-c7a91f54d74a
c983baec-2781-4580-85d6-664d39ad5011	a75b253c-da4f-4000-9e70-52805cfbc156
c983baec-2781-4580-85d6-664d39ad5011	1527b444-22b9-4c88-ab8e-a5c88d1fd7f1
c983baec-2781-4580-85d6-664d39ad5011	9990d4df-dd02-47e2-be96-17d0f85e66c0
d4619402-4111-4b3e-a3a9-a1a30948f328	e81b4811-39bf-47dd-8d2d-1cc5181aeb7c
780da918-2cf5-43b3-9292-4fc883dfe88e	e81b4811-39bf-47dd-8d2d-1cc5181aeb7c
c983baec-2781-4580-85d6-664d39ad5011	e81b4811-39bf-47dd-8d2d-1cc5181aeb7c
c983baec-2781-4580-85d6-664d39ad5011	2ce9dfec-12ee-469b-9a6b-2d9a07d9991d
c983baec-2781-4580-85d6-664d39ad5011	ff3ed73c-ea99-45ef-8aef-6b7b2d217b6e
d4619402-4111-4b3e-a3a9-a1a30948f328	bae712b3-49ce-42ce-8e7a-b795ae100ae4
780da918-2cf5-43b3-9292-4fc883dfe88e	bae712b3-49ce-42ce-8e7a-b795ae100ae4
d4619402-4111-4b3e-a3a9-a1a30948f328	62ed7d0b-2c6e-4203-b3bd-c07ab9fd7c71
780da918-2cf5-43b3-9292-4fc883dfe88e	62ed7d0b-2c6e-4203-b3bd-c07ab9fd7c71
d4619402-4111-4b3e-a3a9-a1a30948f328	414cd3a3-8035-4be5-9f07-550c12e9cd65
780da918-2cf5-43b3-9292-4fc883dfe88e	414cd3a3-8035-4be5-9f07-550c12e9cd65
780da918-2cf5-43b3-9292-4fc883dfe88e	f6596d7b-4e5c-4c57-ab0a-dcf8970885c3
5b17853d-eded-49c1-952d-b83504dffd27	c07ba9b0-acc1-46e9-afcf-b521279b7a92
c983baec-2781-4580-85d6-664d39ad5011	e4506300-f468-437f-a113-f457757d9767
\.


--
-- TOC entry 3934 (class 0 OID 109002)
-- Dependencies: 219
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.roles (id, role_key, name) FROM stdin;
c983baec-2781-4580-85d6-664d39ad5011	SUPER_ADMIN	Super Admin
780da918-2cf5-43b3-9292-4fc883dfe88e	EDITOR	Editor
d4619402-4111-4b3e-a3a9-a1a30948f328	CONTRIBUTOR	Contributor
5b17853d-eded-49c1-952d-b83504dffd27	VIEWER	Viewer
\.


--
-- TOC entry 3940 (class 0 OID 109070)
-- Dependencies: 225
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (user_id, role_id, granted_at) FROM stdin;
c74494f2-b34e-4404-9cac-c47966db18b0	780da918-2cf5-43b3-9292-4fc883dfe88e	2025-11-11 21:53:53.666099+07
7ea24032-7c82-4ccf-b530-7e997b82145e	780da918-2cf5-43b3-9292-4fc883dfe88e	2025-11-11 21:53:53.666099+07
e7100b5a-6394-46f9-a3cd-44753483156c	5b17853d-eded-49c1-952d-b83504dffd27	2025-11-11 21:53:53.666099+07
9c2a6129-d2dc-4a1a-a630-904c7864e230	5b17853d-eded-49c1-952d-b83504dffd27	2025-11-11 22:25:29.239897+07
a0c68edc-bc2b-49b8-919b-a16dc8d0632c	5b17853d-eded-49c1-952d-b83504dffd27	2025-11-11 22:31:51.304134+07
35a1162e-5d35-414c-bd0b-71c050667ebd	c983baec-2781-4580-85d6-664d39ad5011	2025-11-12 06:42:04.731256+07
1d8b9182-d25d-4150-ab78-1110a569adb0	5b17853d-eded-49c1-952d-b83504dffd27	2025-11-12 06:47:42.389094+07
\.


--
-- TOC entry 3933 (class 0 OID 108987)
-- Dependencies: 218
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, fullname, username, email, password_hash, is_email_verified, blocked_until, created_at, updated_at) FROM stdin;
7ea24032-7c82-4ccf-b530-7e997b82145e	System Admin	admin	admin@example.com	$2a$12$GGUGSJOOJU2uVh6m6GpsXePmODvuFRBAtPCceCM7.lvIhA9s3FbdK	t	\N	2025-11-11 21:53:53.666099+07	2025-11-11 21:53:53.666099+07
c74494f2-b34e-4404-9cac-c47966db18b0	Alice Editor	alice	alice@example.com	$2a$12$wwskCafJvOiDoiUXIsDoZ.Ey4k1bGZhk6Iq2nYMs2TvwVTZeExuLK	t	\N	2025-11-11 21:53:53.666099+07	2025-11-11 21:53:53.666099+07
e7100b5a-6394-46f9-a3cd-44753483156c	Bob Viewer	bob	bob@example.com	$2a$12$Ub8Z6GbWmYG2YE8U/6SdKuB9qg5BYPzO.REA1Z/ZQUeVusZ36RR62	f	\N	2025-11-11 21:53:53.666099+07	2025-11-11 21:53:53.666099+07
9c2a6129-d2dc-4a1a-a630-904c7864e230	Andreas Alexander	andreas	drealexanders.dev@gmail.com	$2a$12$/6rQlVPfslnTg82ffvb1V.ua5nHt7LjmC0rfkW.LgLoqhYoQXHL9.	f	\N	2025-11-11 22:25:29.181517+07	2025-11-11 22:25:29.181606+07
a0c68edc-bc2b-49b8-919b-a16dc8d0632c	Andreas Alexanders	andreases	drealexander2.dev@gmail.com	$2a$12$eq8lTObb52Up8xlYPVSWyeEhhBVy1kjprniYBpJvGC.DnNlSXeaHe	f	\N	2025-11-11 22:31:51.098795+07	2025-11-11 22:31:51.098838+07
35a1162e-5d35-414c-bd0b-71c050667ebd	Andreas Alexander	andreas3	drealexander.dev@gmail.com	$2a$12$AVJ01yCOJmu9aB1LUgZUr.RrggV.zgqrHJeheJpaX2KVAstbegk.a	t	2025-11-11 23:50:18.921505+07	2025-11-11 22:50:12.609185+07	2025-11-11 22:50:12.60921+07
1d8b9182-d25d-4150-ab78-1110a569adb0	Andreas Alexander	andreas4	andreas.alexander991@gmail.com	$2a$12$5rrhx6MvrUy82s9sPGFEAOW8JI.YWfEBrifDkxAmLmPRYch94wkbK	t	\N	2025-11-12 06:47:42.338733+07	2025-11-12 06:47:42.338761+07
\.


--
-- TOC entry 3949 (class 0 OID 0)
-- Dependencies: 223
-- Name: login_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.login_attempts_id_seq', 17, true);


--
-- TOC entry 3757 (class 2606 OID 109022)
-- Name: articles articles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_pkey PRIMARY KEY (id);


--
-- TOC entry 3760 (class 2606 OID 109036)
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 3763 (class 2606 OID 109050)
-- Name: auth_otps auth_otps_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auth_otps
    ADD CONSTRAINT auth_otps_pkey PRIMARY KEY (id);


--
-- TOC entry 3767 (class 2606 OID 109064)
-- Name: login_attempts login_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.login_attempts
    ADD CONSTRAINT login_attempts_pkey PRIMARY KEY (id);


--
-- TOC entry 3773 (class 2606 OID 109115)
-- Name: permissions permissions_permission_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_permission_key_key UNIQUE (permission_key);


--
-- TOC entry 3775 (class 2606 OID 109113)
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- TOC entry 3777 (class 2606 OID 109120)
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- TOC entry 3753 (class 2606 OID 109009)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- TOC entry 3755 (class 2606 OID 109011)
-- Name: roles roles_role_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_role_key_key UNIQUE (role_key);


--
-- TOC entry 3771 (class 2606 OID 109075)
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- TOC entry 3747 (class 2606 OID 109001)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 3749 (class 2606 OID 108997)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 3751 (class 2606 OID 108999)
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- TOC entry 3758 (class 1259 OID 109086)
-- Name: idx_articles_author_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_articles_author_id ON public.articles USING btree (author_id);


--
-- TOC entry 3761 (class 1259 OID 109087)
-- Name: idx_audit_logs_actor; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_actor ON public.audit_logs USING btree (actor_user_id);


--
-- TOC entry 3764 (class 1259 OID 109088)
-- Name: idx_auth_otps_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_auth_otps_user_id ON public.auth_otps USING btree (user_id);


--
-- TOC entry 3765 (class 1259 OID 109089)
-- Name: idx_login_attempts_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_login_attempts_user_id ON public.login_attempts USING btree (user_id);


--
-- TOC entry 3768 (class 1259 OID 109091)
-- Name: idx_user_roles_role_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_role_id ON public.user_roles USING btree (role_id);


--
-- TOC entry 3769 (class 1259 OID 109090)
-- Name: idx_user_roles_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_roles_user_id ON public.user_roles USING btree (user_id);


--
-- TOC entry 3786 (class 2620 OID 109138)
-- Name: users trg_grant_viewer_on_user_insert; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_grant_viewer_on_user_insert AFTER INSERT ON public.users FOR EACH ROW EXECUTE FUNCTION public.grant_viewer_on_user_insert();


--
-- TOC entry 3778 (class 2606 OID 109023)
-- Name: articles articles_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id);


--
-- TOC entry 3779 (class 2606 OID 109037)
-- Name: audit_logs audit_logs_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(id);


--
-- TOC entry 3780 (class 2606 OID 109051)
-- Name: auth_otps auth_otps_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auth_otps
    ADD CONSTRAINT auth_otps_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 3781 (class 2606 OID 109065)
-- Name: login_attempts login_attempts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.login_attempts
    ADD CONSTRAINT login_attempts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 3784 (class 2606 OID 109126)
-- Name: role_permissions role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE CASCADE;


--
-- TOC entry 3785 (class 2606 OID 109121)
-- Name: role_permissions role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- TOC entry 3782 (class 2606 OID 109081)
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- TOC entry 3783 (class 2606 OID 109076)
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


-- Completed on 2025-11-12 20:47:18 WIB

--
-- PostgreSQL database dump complete
--

