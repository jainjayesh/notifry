import web
from mako.lookup import TemplateLookup
from google.appengine.ext import db
import datetime
import simplejson as json

class Renderer:
	def __init__(self, templatedir):
		self.template_lookup = TemplateLookup(directories=[templatedir])
		self.data = {}
		self.types = {}
		self.format = None

	def addData(self, name, value):
		self.data[name] = value
		self.types[name] = 'data'

	def addDataList(self, name, value):
		self.data[name] = value
		self.types[name] = 'data-list'

	def addTemplate(self, name, value):
		self.data[name] = value
		self.types[name] = 'template'

	def prepareToJson(self, data):
		if data == None:
			return None
		elif isinstance(data, dict):
			result = {}
			for key in data:
				result[key] = self.prepareToJson(data[key])
			return result
		elif isinstance(data, list) or isinstance(data, db.Query):
			result = []
			for item in data:
				result.append(self.prepareToJson(item))
			return result
		elif isinstance(data, db.Model):
			return self.prepareToJson(data.dict())
		elif isinstance(data, datetime.datetime):
			# Return dates in ISO 8601 format. Always in UTC.
			return data.isoformat()
		elif isinstance(data, int) or isinstance(data, float) or isinstance(data, bool):
			return data
		else:
			return str(data)

	def get_mode(self):
		if self.format:
			return self.format

		inputdata = web.input(format='html')
		format = inputdata.format
		if format != 'json' and format != 'html':
			format = 'html'

		self.format = format
		return format

	def render(self, template_name):
		# Determine the mode from the parameters.
		if self.get_mode() == 'html':
			self.addTemplate('uri', web.url())
			template = self.template_lookup.get_template(template_name)
			return template.render(**self.data)
		elif self.get_mode() == 'json':
			# Filter the data. Only data/data-list go out.
			outputdata = {}
			web.header('Content-Type', 'application/json')
			for key, value in self.data.iteritems():
				if self.types[key] == 'data' or self.types[key] == 'data-list':
					outputdata[key] = self.prepareToJson(value)
			return json.dumps(outputdata)
		else:
			# Not a supported mode.
			return None
