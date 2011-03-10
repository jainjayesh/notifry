import web
from google.appengine.api import users
from google.appengine.ext import db
from lib.Renderer import Renderer
from model.UserSource import UserSource
from model.UserDevice import UserDevice
import datetime

urls = (
	'/', 'index',
	'/login', 'login',
	'/logout', 'logout',
	'/sources/(.*)', 'sources',
	'/registration', 'registerdevice'
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')
renderer.addTemplate('user', users.get_current_user())

# Source editor form.
source_editor_form = web.form.Form(
	web.form.Hidden('id'),
	web.form.Textbox('title', web.form.notnull, description = 'Title'),
	web.form.Textarea('description', description = 'Description'),
	web.form.Checkbox('enabled', description = 'Enabled'),
	web.form.Button('Save')
)

# Front page.
class index:
	def GET(self):
		renderer.addData('data', 'World')
		renderer.addData('other', [1, 2, 3])
		return renderer.render('index.html')

# Login
class login:
	def GET(self):
		user = users.get_current_user()

		if user:
			# Is logged in.
			raise web.seeother('/')
		else:
			# Not logged in - redirect to login.
			raise web.seeother(users.create_login_url(web.url()))

# Logout
class logout:
	def GET(self):
		raise web.seeother(users.create_logout_url("/"))

# Register my device.
class registerdevice:
	def GET(self):
		# For debugging, call POST.
		return self.POST()

	def POST(self):
		# You must be logged in.
		# And we need the following variables.
		# The defaults are provided below.
		input = web.input(devicekey = None, devicetype = None, id = None, deviceversion = None)

		# We must have the following keys passed,
		# otherwise this is an invalid request.
		if not input.devicekey and not input.devicetype:
			# Fail with error.
			pass

		# If ID supplied, find and update that ID.
		device = UserDevice()
		if input.id:
			# Load device from ID.
			device = UserDevice.get_by_id(long(input.id))

		device.updated = datetime.datetime.now()
		device.owner = users.get_current_user()
		device.deviceKey = input.devicekey
		device.deviceType = input.devicetype
		device.deviceVersion = input.deviceversion

		device.put()

		renderer.addData('device', device)
		renderer.addData('test', { 'foo' : 1, 'bar' : True, 'string' : 'foo'})
		return renderer.render('device/registration.html')

# Sources list.
class sources:
	def GET(self, action):
		source = self.get_source()

		if action == 'create' or action == 'edit':
			renderer.addTemplate('action', action)
			
			source_editor = source_editor_form()
			source_editor.fill(source.dict())

			renderer.addTemplate('form', source_editor)
			return renderer.render('sources/edit.html')
		elif action == 'get':
			# Just get the object.
			renderer.addData('source', source.dict())
			renderer.addTemplate('templatesource', source)
			return renderer.render('sources/detail.html')
		else:
			# List.
			sources = UserSource.all()
			sources.filter('owner = ', users.get_current_user())
			sources.order('title')

			renderer.addTemplate('sources', sources)
			return renderer.render('sources/list.html')

	def POST(self, action):
		source = self.get_source()

		source_editor = source_editor_form()
		source_editor.fill(source.dict())

		if not source_editor.validates():
			renderer.addTemplate('action', action)
			renderer.addTemplate('form', source_editor)
			errors = source_editor.getnotes()
			renderer.addDataList('errors', errors)
			return renderer.render('sources/edit.html')
		else:
			# Validated - proceed.
			source.updated = datetime.datetime.now()
			source.title = source_editor.title.get_value()
			source.description = source_editor.description.get_value()
			source.enabled = False
			source.owner = users.get_current_user()
			if source_editor.enabled.get_value():
				source.enabled = True
			source.put()

			# Redirect to the source list.
			web.seeother('/sources/')

	def get_source(self):
		input = web.input(id=None)
		if input.id:
			# Load source by ID.
			source = UserSource.get_by_id(long(input.id))
			# Error handling here.
			return source
		else:
			# New source.
			source = UserSource()
			source.new_object()
			return source

# Initialise and run the application.
app = web.application(urls, globals())
main = app.cgirun()