import os
import random

from datetime import datetime, timedelta
from time import sleep

from sqlalchemy import create_engine, text

if __name__ == "__main__":
    DB_URI = os.getenv("DB_URI", None)
    if DB_URI is None:
        print("DB_URI env var not set. Please set it with a valid URI for sql alchemy")
    engine = create_engine(DB_URI)

    # for deterministically creating simulated sales data
    r = random.Random(x=42)

    # start generating new orders from the latest timestamp
    current_ts: datetime = None
    with engine.connect() as conn:
        current_ts = conn.execute(text("select max(timestamp) from sale")).fetchone()[0]

        if current_ts is None:
            current_ts = datetime(year=2020, month=1, day=1)

    # go on adding new records to the table
    while True:
        current_ts += timedelta(seconds=r.randint(1, 1000))
        data = {
            "id": None,
            "product_id": r.randint(1, 100),
            "user_id": r.randint(2001, 2100),
            "price": r.randint(1, 1000),
            "timestamp": current_ts
        }
        with engine.connect() as conn:
            conn.execute(text("insert into sale values(:id, :product_id, :user_id, :price, :timestamp)"), data)
            conn.commit()
            print(data)
        sleep(5)
