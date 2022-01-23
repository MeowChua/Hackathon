import requests  
from selenium import webdriver
from bs4 import BeautifulSoup

def get_html_source_by_selenium(url):
    driver_chrome = webdriver.Chrome("chromedriver.exe")
    driver_chrome.get(url)
    return driver_chrome.page_source


def get_url (filePath):
	searchUrl = 'http://www.google.com/searchbyimage/upload'
	multipart = {'encoded_image': (filePath, open(filePath, 'rb')), 'image_content': ''}
	response = requests.post(searchUrl, files = multipart, allow_redirects = False)
	fetchUrl = response.headers['Location'] 
	return fetchUrl
	
if __name__ == "__main__":

	filename = "F:/Travel_BlockChain/img/capTreoNhaTrang.png"
	url = get_url(filename)
	page = get_html_source_by_selenium(url)
	soup = BeautifulSoup(page, 'html.parser')

	try:
		title_link_part = soup.find_all("div", {"class": "r5a77d"})
		title_link_part[0].a.text
		print(title_link_part[0].a.text)

	except Exception:
		print("Could not crawl")