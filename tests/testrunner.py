from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
from itertools import izip

import unittest
import commands
import sys
import os

PACKAGE         = 'org.getlantern.firetweet'
ACTIVITY        = 'org.mariotaku.twidere.activity.MainActivity'
APK_PATH        = 'firetweet.apk'

SCREENSHOTS             = "screenshots"
SUCCESS_SCREENSHOTS     = "success"

TWITTER_USERNAME    = 'ftmonkeyrunner'
TWITTER_PASSWORD    = 'pass87'

device = None

def snapshot(name):
    result = device.takeSnapshot()
    result.writeToFile(SCREENSHOTS + '/' + name + '.png', 'png')
    print image_diff(SCREENSHOTS + '/' + name + '.png', SUCCESS_SCREENSHOTS + '/' + name + '.png')
    return True

def image_diff(imageA, imageB):
    # See http://rosettacode.org/wiki/Percentage_difference_between_images
    i1 = MonkeyRunner.loadImageFromFile(imageA)
    i2 = MonkeyRunner.loadImageFromFile(imageB)
    return i1.sameAs(i2, 0.9)

class TestFiretweet(unittest.TestCase):
    def test_001_setup(self):
        assert(os.path.exists(SCREENSHOTS))
    def test_002_connect(self):
        global device
        device = MonkeyRunner.waitForConnection()
    def test_003_remove_app(self):
        global device
        device.shell('am force-stop ' + PACKAGE)
        device.removePackage(PACKAGE)
    def test_004_install_package(self):
        global device
        device.installPackage(APK_PATH)
        MonkeyRunner.sleep(10)
    def test_005_launch_app(self):
        global device
        device.startActivity(component=PACKAGE+'/'+ACTIVITY)
        MonkeyRunner.sleep(30)
        assert(snapshot('test_launch_app'))
    def test_006_write_username(self):
        global device
        device.type(TWITTER_USERNAME)
        MonkeyRunner.sleep(5)
        assert(snapshot('test_write_username'))
    def test_007_write_invalid_password(self):
        global device
        # Focusing the password input.
        device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
        device.press('KEYCODE_DPAD_CENTER', MonkeyDevice.DOWN_AND_UP)
        device.type(TWITTER_PASSWORD + 'invalid')
        MonkeyRunner.sleep(5)
        assert(snapshot('test_write_invalid_password'))
    def test_008_invalid_login(self):
        global device
        # Focusing the login button.
        device.press ('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
        device.press ('KEYCODE_DPAD_RIGHT', MonkeyDevice.DOWN_AND_UP)
        device.press ('KEYCODE_DPAD_CENTER', MonkeyDevice.DOWN_AND_UP)
        MonkeyRunner.sleep(5)
        assert(snapshot('test_invalid_login_1'))
        MonkeyRunner.sleep(40)
        assert(snapshot('test_invalid_login_2'))
    def test_009_fix_password(self):
        global device
        device.press ('KEYCODE_DPAD_LEFT', MonkeyDevice.DOWN_AND_UP)
        device.press ('KEYCODE_DPAD_UP', MonkeyDevice.DOWN_AND_UP)
        device.press ('KEYCODE_DPAD_CENTER', MonkeyDevice.DOWN_AND_UP)

        device.press('KEYCODE_SHIFT_LEFT', MonkeyDevice.DOWN)

        for i in range(20):
            device.press('KEYCODE_DPAD_LEFT', MonkeyDevice.DOWN_AND_UP)
            MonkeyRunner.sleep(1)

        device.press('KEYCODE_SHIFT_LEFT', MonkeyDevice.UP)
        device.press('KEYCODE_DEL', MonkeyDevice.DOWN_AND_UP)

        device.type(TWITTER_PASSWORD)
        MonkeyRunner.sleep(5)
        assert(snapshot('test_fix_password'))
    def test_010_valid_login(self):
        global device
        device.press ('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
        device.press ('KEYCODE_DPAD_RIGHT', MonkeyDevice.DOWN_AND_UP)
        device.press ('KEYCODE_DPAD_CENTER', MonkeyDevice.DOWN_AND_UP)

        MonkeyRunner.sleep(60)
        assert(snapshot('test_valid_login'))

if __name__ == '__main__':
    if not os.path.exists(SCREENSHOTS):
        os.makedirs(SCREENSHOTS)
    suite = unittest.TestLoader().loadTestsFromTestCase(TestFiretweet)
    unittest.TextTestRunner(verbosity=2).run(suite)
