from google.appengine.ext import db

class UserDevice(db.Model):
	owner = db.UserProperty(required=True)
	deviceKey = db.StringProperty(required=True)
	created = db.DateTimeProperty(required=True)
	updated = db.DateTimeProperty(required=True)
	deviceType = db.StringProperty(required=True)
	deviceVersion = db.StringProperty(required=True)