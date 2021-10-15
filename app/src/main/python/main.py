from PIL import Image
#from flask import Flask, request, jsonify
import cv2
import numpy as np
#from pyzbar import pyzbar
import requests
import time
import json
import base64
import io
import qrdecode
# import the necessary packages
import os
from os.path import dirname, join
from com.chaquo.python import Python

def process_image(input_frame):
    # Read the image via file.stream
    frame = Image.open(io.BytesIO(bytes(input_frame)))
    img_np= np.array(frame)
    img_np = cv2.resize(img_np,(img_np.shape[1]//2,img_np.shape[0]//2))
    files_dir = str(Python.getPlatform().getApplication().getFilesDir())
    fname = join(dirname(files_dir),"f.jpeg")
    with open(fname, 'w',encoding='utf8', errors='ignore') as fin:
        fin.write(str(img_np))


    original = img_np.copy()
    gray = cv2.cvtColor(img_np, cv2.COLOR_BGR2GRAY)
    blur = cv2.GaussianBlur(gray, (9,9), 0)
    thresh = cv2.threshold(blur, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)[1]
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5,5))
    close = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel, iterations=3)

    cnts = cv2.findContours(close, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    cnts = cnts[0] if len(cnts) == 2 else cnts[1]
    for c in cnts:
        peri = cv2.arcLength(c, True)
        approx = cv2.approxPolyDP(c, 0.04 * peri, True)
        x,y,w,h = cv2.boundingRect(approx)
        area = cv2.contourArea(c)
        ar = w / float(h)
        if len(approx) == 4 and area > 8000 and (ar > .85 and ar < 1.3):
            cv2.rectangle(img_np, (x, y), (x + w, y + h), (36,255,12), 3)
            ROI = original[y-5:y+h+5, x-5:x+w+5]


    try:
        img_np = ROI.copy()
        gray = cv2.cvtColor(img_np, cv2.COLOR_BGR2GRAY)
        for i in range(len(img_np)):
            for j in range(len(img_np[0])):
                invertedPixel = (0xFFFFFF - gray[i][j]) | 0xFF000000
                img_np[i][j] = invertedPixel
        cv2.imwrite(fname, img_np)
        x = cv2.imread(fname)
        return fname
    except:
        return 'QR detection fail'


    url = 'https://qrzbar.herokuapp.com/im_size'

    my_img = {'image': open(fname,"rb")}
    # try:
    #     r = requests.post(url, files=my_img)
    # # convert server response into JSON format.
    #     print("RESult : " + str(r.json()))
    #
    # except:
    #     print("Connection refused by the server..")
    #     time.sleep(5)
    #     print("Was a nice sleep, now let me continue...")


    flag=1
    gray = cv2.cvtColor(img_np, cv2.COLOR_BGR2GRAY)
    for i in range(len(img_np)):
        for j in range(len(img_np[0])):
            invertedPixel = (0xFFFFFF - gray[i][j]) | 0xFF000000
            img_np[i][j] = invertedPixel

    l =[]
    for i in range(50,500,50):
        img_cp = cv2.resize(img_np,(i,i))
        l.append(img_cp)


    for i in l:
        qrDecoder = cv2.QRCodeDetector()
        data,bbox,rectifiedImage = qrDecoder.detectAndDecode(i)
        #barcodes = pyzbar.decode(i)
        barcode_info = "NONE"
        # for barcode in barcodes:
        #     x, y , w, h = barcode.rect
        barcode_info = data
        if barcode_info!="":
            print("YES")
            return barcode_info
 #           break

    if barcode_info=="":
        return str(base64.b64encode(cv2.imencode('.jpg',  img_np)[1]))


    url = 'https://nfc-project-rvce.herokuapp.com/api/qrcode'
    payload = {"code":barcode_info,"lat":"12.9564672","long":"77.594624"}
    r = requests.post(url, json=payload)
    # convert server response into JSON format.
    try:
        print(r.json())
        if (r.json()['msg']=='success'):
            if r.json()['data']==None:
                return {'msg': 'success', 'info': 'None'}
            else:
                return {'msg': 'success', 'info': r.json()['data'] }
    except:
        print("2nd API ERROR!!!")




    return {'msg': '2nd Level API failed or UID doesnt exist', 'info': 'None'}