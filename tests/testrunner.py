from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice

import unittest
import commands
import sys
import os

PACKAGE         = 'org.getlantern.firetweet'
ACTIVITY        = 'org.getlantern.firetweet.activity.MainActivity'
APK_PATH        = 'firetweet.apk'

if os.environ.get('APK_PATH') is not None:
    APK_PATH = os.environ['APK_PATH']

SCREENSHOTS             = "screenshots"
SUCCESS_SCREENSHOTS     = "success"

TWITTER_USERNAME    = 'ftmonkeyrunner'
TWITTER_PASSWORD    = 'pass87'

device = None

def tear_up():
    if not os.path.exists(SCREENSHOTS):
        os.makedirs(SCREENSHOTS)

def snapshot(name):
    result = device.takeSnapshot()
    result.writeToFile(SCREENSHOTS + '/' + name + '.png', 'png')
    return image_diff(SCREENSHOTS + '/' + name + '.png', SUCCESS_SCREENSHOTS + '/' + name + '.png')

def image_diff(imageA, imageB):
    # See http://rosettacode.org/wiki/Percentage_difference_between_images
    i1 = MonkeyRunner.loadImageFromFile(imageA)
    if not os.path.isfile(imageB):
        return True
    i2 = MonkeyRunner.loadImageFromFile(imageB)
    return i1.sameAs(i2, 0.9)

class TestFiretweet(unittest.TestCase):
    def test_001_setup(self):
        assert(os.path.exists(SCREENSHOTS))
    def test_002_connect(self):
        global device
        device = MonkeyRunner.waitForConnection(120, ".*")
        assert(device is not None)
        try:
            # Lock device
            device.press("POWER", MonkeyDevice.DOWN_AND_UP)
            MonkeyRunner.sleep(10)
            # Unlock device
            device.wake()
            MonkeyRunner.sleep(10)
            device.drag((540, 1900), (540, 960), 1.0, 120)
        except Exception:
            fail("Could not unlock screen.")
    def test_003_remove_app(self):
        global device
        device.shell('killall com.android.commands.monkey')
        device.shell('am force-stop ' + PACKAGE)
        device.removePackage(PACKAGE)
        MonkeyRunner.sleep(10)
    def test_004_install_package(self):
        global device
        device.installPackage(APK_PATH)
        MonkeyRunner.sleep(10)
    def test_005_launch_app(self):
        global device

        try:
            device.startActivity(component=PACKAGE+'/'+ACTIVITY)
        except Exception:
            fail("Could not start activity.")

        MonkeyRunner.sleep(20)
        assert(snapshot('test_launch_app'))
    def test_006_write_username(self):
        global device

        try:
            device.type(TWITTER_USERNAME)
        except Exception:
            fail("Could not type username.")

        MonkeyRunner.sleep(5)
        assert(snapshot('test_write_username'))
    def test_007_write_invalid_password(self):
        global device

        try:
            # Focusing the password input.
            device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
            device.press('KEYCODE_DPAD_CENTER', MonkeyDevice.DOWN_AND_UP)
            # Typing password.
            device.type(TWITTER_PASSWORD + 'invalid')
        except Exception:
            fail("Could not type password.")

        MonkeyRunner.sleep(5)
        assert(snapshot('test_write_invalid_password'))
    def test_008_invalid_login(self):
        global device

        try:
            # Focusing the login button.
            device.press ('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
            device.press ('KEYCODE_DPAD_RIGHT', MonkeyDevice.DOWN_AND_UP)
            device.press ('KEYCODE_DPAD_CENTER', MonkeyDevice.DOWN_AND_UP)
        except Exception:
            fail("Could not touch login button.")

        MonkeyRunner.sleep(5)
        assert(snapshot('test_invalid_login_1'))
        MonkeyRunner.sleep(50)
        assert(snapshot('test_invalid_login_2'))
    def test_009_fix_password(self):
        global device

        try:
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
        except Exception:
            fail("Could not retype password.")

        MonkeyRunner.sleep(5)
        assert(snapshot('test_fix_password'))
    def test_010_valid_login(self):
        global device

        try:
            device.press ('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
            device.press ('KEYCODE_DPAD_RIGHT', MonkeyDevice.DOWN_AND_UP)
            device.press ('KEYCODE_DPAD_CENTER', MonkeyDevice.DOWN_AND_UP)
        except Exception:
            fail("Could not touch login button.")

        MonkeyRunner.sleep(50)
        assert(snapshot('test_valid_login'))

if __name__ == '__main__':
    tear_up()
    suite = unittest.TestLoader().loadTestsFromTestCase(TestFiretweet)
    unittest.TextTestRunner(verbosity=2).run(suite)
