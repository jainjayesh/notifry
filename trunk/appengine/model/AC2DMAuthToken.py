from google.appengine.ext import db
from google.appengine.api.urlfetch import fetch
import urllib
import datetime

class AC2DMTokenException(Exception):
	pass

class AC2DMAuthToken(db.Model):
	token = db.StringProperty()
	created = db.DateTimeProperty(auto_now_add=True)
	updated = db.DateTimeProperty()
	comment = db.StringProperty()

	def new_object(self):
		pass

	def dict(self):
		result = {
			'token': self.token,
			'created': self.created,
			'updated': self.updated,
			'comment': self.comment
		}

		try:
			result['id'] =  self.key().id()
		except db.NotSavedError, ex:
			# Not saved yet, so it has no ID.
			pass

		return result

	@staticmethod
	def get_latest():
		query = AC2DMAuthToken.all()
		query.order('-updated')

		if query.count(5) > 0:
			return query[0]
		else:
			return None

	@staticmethod
	def from_username_password(username, password):
		# Attempt to create a token from a username and password.
		params = {}
		params['accountType'] = 'HOSTED_OR_GOOGLE'
		params['Email'] = username
		params['Passwd'] = password
		params['service'] = 'ac2dm'
		params['source'] = 'Notifry - 0.1'

		result = fetch(
			"https://www.google.com/accounts/ClientLogin",
			urllib.urlencode(params), # POST body
			"POST", # HTTP method
			{}, # Additional headers
			False, # Don't allow a truncated response
			True, # Do follow redirects.
			None, # Default timeout/deadline
			True # Validate the SSL certificate/issuer/time (important!)
		)

		if result.status_code == 200:
			# Success!
			token = AC2DMAuthToken()
			token.new_object()
			token.updated = datetime.datetime.now()

			bits = result.content.split("\n")
			for line in bits:
				if line.startswith("Auth="):
					token.token = line[5:]
					token.comment = "Created with username and password in admin area."

			return token
		else:
			# Failure. Throw exception.
			raise AC2DMTokenException('Failed to get token: Response code ' + str(result.status_code) + ' with content ' + result.content)