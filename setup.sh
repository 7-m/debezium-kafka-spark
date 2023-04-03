export KAFKA_BROKERS_URI="localhost:29092"

# app-id to identify consumer group in kafka streams
export APP_ID="APPID-4"

# topic to which records from debezium will be written to
# should be of the format <dbzium_topic.prefix>.<db_name>.<table_name>
export DBZ_TOPIC="mydbv4.inventory.sale"

# topic to which transformed records will be written by kstreams and read by spark
export SPARK_TOPIC="topic2"

# database uri as understood by sqlalchemy
export DB_URI="mysql+pymysql://root:password@localhost:3306/inventory"