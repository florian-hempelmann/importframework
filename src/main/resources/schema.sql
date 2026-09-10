CREATE TABLE IF NOT EXISTS wheretobuy (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        "group" TEXT,
        address TEXT,
        city TEXT,
        zipcode TEXT,
        phone TEXT,
        fax TEXT,
        email TEXT,
        url TEXT,
        onlineshopname TEXT,
        has_online_shop BOOLEAN,
        has_local_shop BOOLEAN,
        autocomplete_name TEXT,
        latitude REAL,
        longitude REAL
        );