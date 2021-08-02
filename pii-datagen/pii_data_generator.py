
import os
import logging
import json
import time
import random
from confluent_kafka import Producer
from faker import Faker
from faker.providers import credit_card

# Static list of users to match the users created when starting Confluent
KAFKA_USERS = [
    { "full_name": "bob jones", "ssn": "111-11-1111" },
    { "full_name": "alice smith", "ssn": "222-22-2222" },
    { "full_name": "john hernandez", "ssn": "333-33-3333" }
]

LOGGER_NAME = "pii_data_generator"
LOG_LEVEL = "INFO"

logger = logging.getLogger(LOGGER_NAME)
logger.setLevel(LOG_LEVEL)
stream_handler = logging.StreamHandler()
stream_handler.setLevel(LOG_LEVEL)
formatter = logging.Formatter('%(asctime)s :: %(name)s :: %(levelname)-8s :: %(message)s')
stream_handler.setFormatter(formatter)
logger.addHandler(stream_handler)

def generate_mock_pii_data(fake_data):
    # Choose random user from list above
    kafka_user = KAFKA_USERS[random.randint(0, 2)]
    full_name_split = kafka_user["full_name"].split()
    return {
        "full_name": kafka_user["full_name"],
        "first_name": full_name_split[0],
        "last_name": full_name_split[1],
        "email": f"{full_name_split[0]}{full_name_split[1]}@yahoo.com",
        "ssn": kafka_user["ssn"],
        "address": fake_data.address().replace("\n", " "),
        "credit_card": {
            "provider": fake_data.credit_card_provider(),
            "number": fake_data.credit_card_number(),
            "expiration_date": fake_data.credit_card_expire(),
            "security_code": fake_data.credit_card_security_code()
        }
    }

def acked(err, msg):
    if err is not None:
        logger.error(
            'Failed to deliver message - Topic: %s, Error Name: %s, Error Code: %s, Error Message: %s',
            msg.topic(), err.name(), err.code(), err.str()
        )
    else:
        logger.info("Message produced - Topic: %s, Offset: %s", msg.topic(), msg.offset())

def create_kafka_producer(kafka_url, topic, username, password):
    if(kafka_url is None or topic is None or username is None or password is None):
        logger.error("Please configure the following environment variables: BOOTSTRAP_SERVERS, KAFKA_TOPIC, SASL_USERNAME, SASL_PASSWORD")
        return None

    conf = {
        "bootstrap.servers": os.environ.get("BOOTSTRAP_SERVERS"),
        "client.id": "pii_data_generator",
        "security.protocol": "sasl_plaintext",
        "sasl.mechanism": "PLAIN",
        "sasl.username": os.environ.get("SASL_USERNAME"),
        "sasl.password": os.environ.get("SASL_PASSWORD")
    }

    return Producer(conf)

if __name__ == "__main__":
    bootstrap_servers = os.environ.get("BOOTSTRAP_SERVERS")
    kafka_topic = os.environ.get("KAFKA_TOPIC")
    sasl_username = os.environ.get("SASL_USERNAME")
    sasl_password = os.environ.get("SASL_PASSWORD")

    kafka_producer = create_kafka_producer(bootstrap_servers, kafka_topic, sasl_username, sasl_password)

    fake = Faker()
    fake.add_provider(credit_card)

    if kafka_producer is not None:
        try:
            while True:
                kafka_producer.produce(kafka_topic, value=json.dumps(generate_mock_pii_data(fake)), callback=acked)
                time.sleep(2)
                kafka_producer.poll(1)
        except KeyboardInterrupt:
            logger.info("KeyboardInterrupt - Exiting application")
        finally:
            logger.info("Closing Kafka Producer")
            if kafka_producer is not None:
                kafka_producer.flush()
