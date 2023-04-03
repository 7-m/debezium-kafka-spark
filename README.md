# MySQL + Debezium + Kafka + Spark
## Preface 
This project is a demonstration of multiple technologies to showcase how a streaming reporting dashboard can be buillt. It uses a MySQL DB to simulate a production database, kafka + kafka connect + debezium to replicate the contents of the database into kafka and spark to for reporting.

## Architecture
1. Replicate data to Kafka using Debezium
2. (same as above)
3. Use Kafka Streams API to extract the payload from Debeziums message --
4. -- write it back to a new kafka topic
5. create reports using apache spark
```
┌───────────────┐
│               │
│               │
│  MySQL DB     │
│               │
│               │
│               │        ┌───────────────────┐
└────────────┬──┘        │                   │
             │           │  Kafka Connect    │
             └──────────►│       +           │  2
              1          │    Debezium       ├────────┐
                         │                   │        │
                         │                   │        ▼
                         │                   │    ┌────────────┐
                         └───────────────────┘    │            │   5        ┌────────────────────┐
                                                  │   Kafka    ├───────────►│                    │
                                                  │            │            │  spark to create   │
                                                  │            │            │    reports         │
                                                  │            │            │                    │
                                                  └┬─────────▲─┘            │                    │
                                                   │         │              └────────────────────┘
                                                  3│        4│
                                                   │         │
                                                   │         │
                                                   │         │
                                                   ▼         │
                                              ┌──────────────┴───────┐
                                              │ Kafka Stream API     │
                                              │                      │
                                              │   to extract payload │
                                              │                      │
                                              └──────────────────────┘
```

## Running it
0. Configure and run `setup.sh` to set env variables before running any of the applications
1. Setup MySQL Database with database called `inventory` and table called `sale`.
2. Create the `sale` table `create table sales(id int primary key auto_increment, product_id int, user_id int, price float, timestamp timestamp);`
3. Run `generate-data.py` to create mock data for `sale`
4. Start Kafka and kafka connect preferably using debeziums docker image.
5. Configure Kafka connect with debezium to replicate the `inventory` database using the following curl
    ```curl
       curl --request POST \
       --url http://localhost:8083/connectors \
       --header 'Accept: application/json' \
       --header 'Content-Type: application/json' \
       --data '{
       "name": "inventory-connector-v2",
       "config": {
       "connector.class": "io.debezium.connector.mysql.MySqlConnector",
       "database.hostname": "localhost",
       "database.port": "3306",
       "database.user": "root",
       "database.password": "password",
       "database.server.id": "184054",
       "topic.prefix": "mydbv4",
       "database.include.list": "inventory",
       "schema.history.internal.kafka.bootstrap.servers": "localhost:29092",
       "schema.history.internal.kafka.topic": "schemahistory.fullfillment",
       "include.schema.changes": "false",
    
        "transforms" : "unwrap",
        "transforms.unwrap.type" : "io.debezium.transforms.ExtractNewRecordState" ,
    
        
        "snapshot.mode": "initial",
        
        "database.allowPublicKeyRetrieval" : true
    
       }
       }'
    ```

    Replace the above config options with right values.
6. Run app `kstream-filter` to transform Debezium's message and save them in a separate topic.
7. 