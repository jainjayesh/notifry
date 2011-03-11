from google.appengine.ext import db

class AC2DMAuthToken(db.Model):
	token = db.StringProperty()
	created = db.DateTimeProperty(auto_now_add=True)
	updated = db.DateTimeProperty()
	comment = db.StringProperty()

def get_latest():
	query = AC2DMAuthToken.all()
	query.order('-updated')

	return query[0]