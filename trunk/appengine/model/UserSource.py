from google.appengine.ext import db

class UserSource(db.Model):
	owner = db.UserProperty(required=True)
	title = db.StringProperty(required=True)
	created = db.DateTimeProperty(required=True)
	updated = db.DateTimeProperty(required=True)
	description = db.StringProperty(multiline=True)
	externalKey = db.StringProperty(required=True)
	enabled = db.BooleanProperty()