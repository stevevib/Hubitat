# Hubitat Onkyo Multi-Zone AVR Driver

This driver controls Onkyo network audio video receivers using the Onkyo eISCP protocal over Hubitat's telnet interface. The driver supports up to 4 zones (Main and Zones 2-4) by creating a separate child device for each selected zone.  Please note that only Onkyo 'Network Receiver' models are supported.  If your receiver have an Ethernet port on the rear panel odds are pretty good it'll work with this driver.
<br>
<br>

## Versions
___
* v0.20.1214.1      (12/14/20): Initial beta release
<br>
<br>

## Installation Instructions
___
<br>

**Add this Parent Device Driver to Your Hubitat Elevation Hub**

1. Open the "OnkyoMultiZoneAVRParent.groovy" driver from this repository. Click the RAW button and select and copy the entirety of the code.

2. Navigate to your Hubitat Elevation Hub's 'Driver Code' page, click the <b>*+New Driver*</b> button located in the upper right corner of the Drivers Code page. 

3. Click the editor window and paste the contents copied in step 1.

4. Click the Save button.

<br>

**Add the Child Device Driver to Your Hubitat Elevation Hub**

1. Open the "OnkyoAVRZoneChild.groovy" driver from this repository.

2. Repeat steps 2-4 from the previous section to add the child driver.

<br>

**Create the 'Onkyo Multi Zone AVR' Hubitat Elevation Device**

1. Navigate to your Hubitat Elevation Hub's web page, select the "Devices" section, and then click the <b>*+Add Virtual Device*</b> button in the top right corner.

2. In the window that appears, fill in the <b>*Device Name*</b>, <b>*Device Label*</b> (optional), and <b>*Device Network Id*</b> fields.  
 For example:
    * <b>*Device Name*</b> = Onkyo TX-NR3010 AVR
    * <b>*Device Label*</b> = Family Room AVR

3. In the <b>*Type*</b> field, scroll to the bottom of the list and the select "Onkyo Multi-Zone AVR Parent" driver from the dropdown list

4. Click the Save button

5. The device preferences window will be displayed for your newly added device. Complete the following settings:

    * <b>*IP address*</b> of your Onkyo AVR
    * <b>*eISCP Port number*</b> (defaults to '60128' and should work for most receivers unless you have specifically set a different port on the Onkyo itself)
    * <b>*eISCP Termination Option*</b> (defaults to 'CR' and should work for most receivers)
    * <b>*Supported Volume Range*</b> (defaults to '0-80 (0x00-0x50)' and should work for most receivers)
    * <b>*Enabled Zones*</b> by Ctrl-Clicking (Windows) each zone you want to enable.
    * Click <b>*Save Preferences*</b> to save the settings and create the child devices. A separate child device will be added for each selected zone.

11. Navigate to your Hubitat Elevation Hub's <b>*Devices*</b> section and find the Device Name (or Device Label if you added text in this field) from the Devices list for the AVR you just added. You should see a child device for each zone you selected on the device's preferences page. If you missed a zone, click on the parent device to go to the preferences page, select all of the zones you'd like to add, and click the Save <b>*Preferences button*</b>. New zones will be added but existing zones will be unaffected.

12. By default, the child driver will automatically name an input source numerically (Input 1, Input 2, etc.) the first time your AVR is switched to a source not known to the driver. You can rename these inputs to match the actual Onkyo input names.  Most Onkyo AVRs have default input names that are fairly generic in nature (Strm Box, BD, CD, etc.) but allow the user to rename the inputs using user defined names that match their actual setup (Roxu, X-Box One, Jukebox, etc.). Renaming the child device input names allows you to match the input names you've set on the AVR itself. 
<br>
<br>

## Notes
___
While this driver will allow you to add up to 4 different child zones (Main, and zones 2 - 4), the capabilities of your specific Onkyo AVR will determine how many physical zones can be controlled.  

For each installed child/zone, the driver supports the following functions:

* Zone Power: On/Off
* Zone Volume Level
* Zone Muting: Muted/Unmuted
* Input Source: selection by index number
* Input Source Naming: allows attaching a name to the input index
* Input Source: by raw 2 character hex value (see Onkyo eISCP Protocol document for model specific input code values)
* The ability to send a raw eISCP command which should allow you additional control above the core functions listed above (see the Onkyo eISCP Protocal Documents in the Docs folder for this driver for addtional information regarding available commands)
<br>
<br>


Onkyo periodically publishes eISCP Protocol documents that includes command codes and associated valid value ranges for the receivers in production at the time the protocal document was published. While the basic commands have generally remained the same over multiple product generations, commands that accept a range of values such as volume level and input source can vary siginificantly from model to model. The protocal documents can be used to determine which commands and values are supported by your specific AVR model.
<br>
<br>

Some receivers may not respond to commands as expected.  I own two Onkyo AVRs; a TX-NR3010 purchased in 2013 and a TX-NR777 purchased in 2017.  The 3010 refuses to respond to volume commands for Zone 2 and returns *ZVLN/A* when any attempt is made to change the volume setting using either a specific volume level or the volume up/down commands.  While this could certainly be related to issues with this driver, the 777 works as expected and other methods I have tried to control the 3010's volume (including Node-RED with the 
node-red-contrib-eiscp node) result in the same *ZVLN/A* response.
<br>
<br>

___
<br>
<br>
Donations are never required but always appreciated. If you feel so inclined, you can help fuel my addiction gadgets, gizmos, good coffee and good beer (and occasionally, coffee flavored beer but <b>never</b> beer flavored coffee!) by visiting: 
https://www.paypal.com/donate?hosted_button_id=QQFKFQZWNM8SG

<br>

<form action="https://www.paypal.com/donate" method="post" target="_top">
<input type="hidden" name="hosted_button_id" value="QQFKFQZWNM8SG" />
<input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif" border="0" name="submit" title="PayPal - The safer, easier way to pay online!" alt="Donate with PayPal button" />
<img alt="" border="0" src="https://www.paypal.com/en_US/i/scr/pixel.gif" width="1" height="1" />
</form>
