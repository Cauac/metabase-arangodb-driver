# python driver docs: https://docs.python-arango.com/en/main/index.html
# datasets docs: https://github.com/arangoml/arangodb_datasets

from arango import ArangoClient
from arango_datasets import Datasets

# use 'host.docker.internal' instead of 'localhost' if accessing from another container
database_host = 'http://localhost:8529'
database_name = 'test_db'
user_name = 'metabase'
user_pass = '1111'

# Connect to "_system" database as root user.
client = ArangoClient(hosts=database_host)
sys_db = client.db("_system", username="root", password="")

# Create a test database if it does not exist.
# Only root user has access to it at time of its creation.
if not sys_db.has_database(database_name):
    sys_db.create_database(database_name)

# Create test user with permission for the test db.
if not sys_db.has_user(user_name):
    sys_db.create_user(
        username=user_name,
        password=user_pass,
        active=True
    )
    sys_db.update_permission(
        username=user_name,
        permission='rw',
        database=database_name
    )

# List all databases.
print('Databases: ', sys_db.databases())
# List all users.
print('Users: ', sys_db.users())

# Connect to the test database as test user.
db = client.db(database_name, username="root", password="")

datasets = Datasets(db)
datasets.load("FLIGHTS")
