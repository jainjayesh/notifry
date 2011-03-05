from google.appengine.ext import db

class UserMessage(db.Model):
	source = db.ReferenceProperty(UserSource)
	timestamp = db.DateTimeProperty(required=True)
	message = db.StringProperty(multiline=True)
	url = db.StringProperty()
	deliveredToGoogle = db.BooleanProperty()
	lastDeliveryAttempt = db.DateTimeProperty()