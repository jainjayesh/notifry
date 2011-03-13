import web
from google.appengine.api import users
from lib.Renderer import Renderer
from model.AC2DMAuthToken import AC2DMAuthToken
from model.AC2DMAuthToken import AC2DMTokenException
import datetime

urls = (
	'/admin/', 'index',
	'/admin/token/(.*)', 'token',
	'/admin/createtoken/', 'createtoken'
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')
renderer.addTemplate('user', users.get_current_user())

# Front page of Admin.
class index:
	def GET(self):
		return renderer.render('admin/index.html')

# Tokens list.
class token:
	def GET(self, action):
		if action == 'create' or action == 'edit':
			token = self.get_token()
			renderer.addTemplate('action', action)
			
			form = self.get_form()
			form.fill(token.dict())

			renderer.addTemplate('form', form)
			return renderer.render('admin/token/edit.html')
		elif action == 'get':
			# Just get the object.
			token = self.get_token()
			renderer.addData('token', token)
			return renderer.render('admin/token/detail.html')
		else:
			# List.
			tokens = AC2DMAuthToken.all()
			tokens.order('-updated')

			renderer.addDataList('tokens', tokens)
			return renderer.render('admin/token/list.html')

	def POST(self, action):
		token = self.get_token()

		# Get the form and the form data.
		form = self.get_form()
		form.fill(token.dict())

		if not form.validates():
			# Failed to validate. Display the form again.
			renderer.addTemplate('action', action)
			renderer.addTemplate('form', form)
			errors = form.getnotes()
			renderer.addDataList('errors', errors)
			return renderer.render('admin/token/edit.html')
		else:
			# Validated - proceed.
			token.updated = datetime.datetime.now()
			token.token = form.token.get_value()
			token.comment = form.comment.get_value()
			token.put()

			if renderer.get_mode() == 'html':
				# Redirect to the list.
				web.found('/admin/token/')
			else:
				# Send back the source data.
				renderer.addData('token', token)
				return renderer.render('apionly.html')

	def get_token(self):
		# Helper function to get the token object from the URL.
		input = web.input(id=None)
		if input.id:
			# Load token by ID.
			token = AC2DMAuthToken.get_by_id(long(input.id))
			if not token:
				# It does not exist.
				web.notfound()

			return token
		else:
			# New source.
			token = AC2DMAuthToken()
			token.new_object()
			return token

	def get_form(self):
		# Token editor form.
		token_form = web.form.Form(
			web.form.Hidden('id'),
			web.form.Textbox('token', web.form.notnull, description = 'Token'),
			web.form.Textarea('comment', description = 'Comment'),
			web.form.Button('Save')
		)
		return token_form()

# Create token.
class createtoken:
	def GET(self):
		form = self.get_form()

		renderer.addTemplate('form', form)
		return renderer.render('admin/token/login.html')

	def POST(self):
		# Get the form and the form data.
		form = self.get_form()

		if not form.validates():
			# Failed to validate. Display the form again.
			renderer.addTemplate('form', form)
			errors = form.getnotes()
			renderer.addDataList('errors', errors)
			return renderer.render('admin/token/login.html')
		else:
			# Validated.
			# Attempt to get an auth token.
			try:
				token = AC2DMAuthToken.from_username_password(form.username.get_value(), form.password.get_value())
				token.put()

				if renderer.get_mode() == 'html':
					# Redirect to the list.
					web.found('/admin/token/')
				else:
					# Send back the source data.
					renderer.addData('token', token)
					return renderer.render('apionly.html')
			except AC2DMTokenException, e:
				# Failed for some reason!
				renderer.addData('error', str(e))
				renderer.addTemplate('form', form)
				return renderer.render('admin/token/login.html')

	def get_form(self):
		login_form = web.form.Form(
			web.form.Textbox('username', web.form.notnull, description = 'Username'),
			web.form.Textbox('password', web.form.notnull, description = 'Password'),
			web.form.Button('Login')
		)
		return login_form()

# Initialise and run the application.
app = web.application(urls, globals())
main = app.cgirun()