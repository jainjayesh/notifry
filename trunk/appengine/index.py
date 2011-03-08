import web
from google.appengine.api import users

urls = (
	'/', 'index',
	'/login', 'login',
	'/logout', 'logout'
)

render = web.template.render('templates/')

class index:
	def GET(self):
		return render.layout(render.index(), users.get_current_user())

class login:
	def GET(self):
		user = users.get_current_user()

		if user:
			# Is logged in.
			raise web.seeother('/')
		else:
			# Not logged in - redirect to login.
			raise web.seeother(users.create_login_url(web.url()))

class logout:
	def GET(self):
		raise web.seeother(users.create_logout_url("/"))

app = web.application(urls, globals())
main = app.cgirun()

