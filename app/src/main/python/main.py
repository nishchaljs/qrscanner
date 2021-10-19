from PIL import Image
import cv2
import numpy as np
import io
from os.path import dirname, join
from com.chaquo.python import Python

def process_image(input_frame):
    # Read the image via file.stream
    frame = Image.open(io.BytesIO(bytes(input_frame)))
    img_np= np.array(frame)
    #img_np = cv2.resize(img_np,(img_np.shape[1]//2,img_np.shape[0]//2))
    files_dir = str(Python.getPlatform().getApplication().getFilesDir())
    fname = join(dirname(files_dir),"f.jpeg")
    with open(fname, 'w',encoding='utf8', errors='ignore') as fin:
        fin.write(str(img_np))
    cv2.imwrite(fname, img_np)
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
        return fname
    except:
        return 'QR detection fail'
    return "NONE"