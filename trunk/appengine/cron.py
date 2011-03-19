import web
from lib.Renderer import Renderer
import datetime
from model.UserMessage import UserMessage

urls = (
	'/cron/deletemessages', 'deletemessages'
)

# Create the renderer and the initial context.
renderer = Renderer('templates/')

# Cron script to delete messages.
class deletemessages:
	def GET(self):
		now = datetime.datetime.now()
		then = now - datetime.timedelta(1)

		# TODO: Make this all more efficient than it is.
		# TODO: Handle the case where the script runs out of time.
		UserMessage.deleteOlderThan(then)

		return renderer.render('cron/deletemessages.html')

# Initialise and run the application.
app = web.application(urls, globals())
main = app.cgirun()