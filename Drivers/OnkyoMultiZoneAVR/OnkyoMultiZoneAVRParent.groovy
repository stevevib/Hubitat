 /*
    Copyright © 2020 Steve Vibert (@SteveV)

    Portions of this code are based on Mike Maxwell's onkyoIP device handler for SmartThings
    taken from this post: https://community.smartthings.com/t/itach-integration/25470/23
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    Onkyo eISCP Protocol Specifications Documents which include zone specific commands can 
    be found at: https://github.com/stevevib/Hubitat/Devices/OnkyoMultiZoneAVR/Docs/


    Version History:
    ================

    Date            Version             By                  Changes
    --------------------------------------------------------------------------------
    2020-12-14      0.9.201214.1        Steve Vibert        Initial Beta Release
    2021-03-20      0.9.210320.1        Steve Vibert        Fix: text logging settings being ignored


    WARNING!
        In addition to controlling basic receiver functionality, this driver also includes 
        the ability to set volume levels and other settings using raw eISCP commands. Some 
        commands such as volume level, allow you to enter a value from a given min/max value 
        range. Randomly trying these commands without fully understanding these values may 
        lead to unintended consequences that could damage your receiver and/or your speakers.

        Please make sure you read *and understand* the eISCP protocal documents before trying 
        a command to see what it does.   
*/

import groovy.transform.Field

metadata 
{
	definition (name: "Onkyo Multi-Zone AVR Parent", namespace: "SteveV", author: "Steve Vibert")
	{
		capability "Initialize"
		capability "Telnet"

        command "refresh"
	}

    preferences 
	{   
		input name: "onkyoIP", type: "text", title: "Onkyo IP", required: true, displayDuringSetup: true
		input name: "eISCPPort", type: "number", title: "EISCP Port", defaultValue: 60128, required: true, displayDuringSetup: true
		input name: "eISCPTermination", type: "enum", options: [[1:"CR"],[2:"LF"],[3:"CRLF"],[4:"EOF"]] ,title: "EISCP Termination Option", required: true, displayDuringSetup: true, description: "Most receivers should work with CR termination"
		input name: "eISCPVolumeRange", type: "enum", options: [[50:"0-50 (0x00-0x32)"],[80:"0-80 (0x00-0x50)"],[100:"0-100 (0x00-0x64)"],[200:"0-100 Half Step (0x00-0xC8)"]],defaultValue:80, title: "Supported Volume Range", required: true, displayDuringSetup: true, description:"(see Onkyo EISCP Protocol doc for model specific values)"
        input name: "enabledReceiverZones", type: "enum", title: "Enabled Zones", required: true, multiple: true, options: [[1: "Main"],[2:"Zone 2"],[3:"Zone 3"],[4: "Zone 4"]]
 		input name: "textLogging",  type: "bool", title: "Enable description text logging ", required: true, defaultValue: true
        input name: "debugOutput", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

//	Command Map Keys
@Field static Map zone1CmdPrefixes = ["Power":"PWR", "Mute":"AMT", "Volume":"MVL", "Input":"SLI"]
@Field static Map zone2CmdPrefixes = ["Power":"ZPW", "Mute":"ZMT", "Volume":"ZVL", "Input":"SLZ"]
@Field static Map zone3CmdPrefixes = ["Power":"PW3", "Mute":"MT3", "Volume":"VL3", "Input":"SL3"]
@Field static Map zone4CmdPrefixes = ["Power":"PW4", "Mute":"MT4", "Volume":"VL4", "Input":"SL4"]
@Field static Map zoneCmdPrefixes = [:]

@Field static List zoneNames = ["N/A", "Main", "Zone 2", "Zone 3", "Zone 4" ]
@Field static Map zoneNumbers = ["Main":1, "Zone 2":2, "Zone 3":3, "Zone 4":4 ] 

def getVersion()
{
    return "0.9.210320.1"
}

void parse(String description) 
{
    writeLogDebug("parse ${description}")
    handleReceiverResponse(description)
}

def handleReceiverResponse(String description) 
{
    writeLogDebug("handleReceiverResponse ${description}")

	if(!description.startsWith("ISCP"))
		return

	// Find the beginning of the actual response including the start character and Onkyo device
	// type character.  For receivers this will always be !1
	Integer pos = description.indexOf('!')

	if(pos == -1)
		return

	// Strip out the "!1" portion of the response -- we We're only interested in the actual command...
	String data = description.substring(pos + 2).trim()
	writeLogDebug("received ISCP response: ${data}")

    // Split the command into prefix and value parts. The command is always be 3 characters long..
    String cmdPre = data.substring(0, 3)
	String cmdVal = data.substring(3)

    writeLogDebug("parent: cmdPre = ${cmdPre} cmdVal = ${cmdVal}")

    // Determine which zone the command belongs to...
    String zoneName = getCommandZoneName(cmdPre)
    Integer zone = zoneNumbers[zoneName] ?: -1 as Integer

    if(zone == -1)
    {
        writeLogDebug("${cmdPre} not supported by any zones.  Skipping...")
        return;        
    }
    
    writeLogDebug("Forwarding command to ${zoneName} (${zone})")

    // Forward the command to the appropriate zone...
    if(zone != -1 && zoneName.length() > 0)
    {
        def child = getChild(zoneName)

        if(child != null)
        {
            def cmdMap = ["zone":zone, "data":data]
            child.forwardResponse(cmdMap)
        }
    }
}

def initialize()
{
	String ip = settings?.onkyoIP as String
	Integer port = settings?.eISCPPort as Integer

	writeLogDebug("ip: ${ip} port: ${port}")

	telnetConnect(ip, port, null, null)
	writeLogDebug("Opening telnet connection with ${ip}:${port}")

	zoneCmdMap = [1:zone1CmdMap, 2:zone2CmdMap, 3:zone3CmdMap, 4:zone4CmdMap]
    zoneCmdPrefixes = [1:zone1CmdPrefixes, 2:zone2CmdPrefixes, 3:zone3CmdPrefixes, 4:zone4CmdPrefixes]

    try 
    {
        childDevices.each { it ->
            it.initialize()
        }
    } 

    catch(e) 
    {
        log.error "initialize caused the following exception: ${e}"
    }
}

void telnetStatus(String message) 
{
	writeLogDebug("${device.getName()} telnetStatus ${message}")
}

def installed()
{
    log.warn "${device.getName()} installed..."
	//initialize()
    updated()
}

def updated()
{
	writeLogInfo("updated...")
    state.version = getVersion()
    unschedule()

	// disable debug logs after 30 min
	if (debugOutput) 
		runIn(1800,logsOff)

    updateChildren()
    //device.updateSetting("enabledReceiverZones",[value:"false",type:"enum"])	

    initialize()
}

void refresh()
{
    writeLogDebug("refresh")
}

def logsOff() 
{
    log.warn "${device.getName()} debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])	
}

private writeLogDebug(msg) 
{
    if (settings?.debugOutput || settings?.debugOutput == null)
        log.debug "$msg"
}

private writeLogInfo(msg)
{
    if (settings?.textLogging || settings?.textLogging == null)
        log.info "$msg"
}

def updateChildren()
{
    writeLogDebug("updateChildren...")

    try 
    {
        writeLogDebug("enabledReceiverZones: ${enabledReceiverZones}")

        enabledReceiverZones.each { it ->
        
            writeLogDebug("Checking if zone ${it} child exists...")
            Integer childZone = it as Integer
            String childName = zoneNames[childZone]
  
            // Get child device...
            def child = getChild(childName)

            // ...or create it if it doesn't exist
            if(child == null) 
            {
                if (logEnable) 
                    writeLogDebug("Child with id ${childName} does not exist.  Creating...")
                
                def childType = "Onkyo Multi-Zone AVR Child Zone"
                createChildDevice(childZone, childName, childType)
                child = getChild(childName)

                if(child != null)
                {
                    //writeLogDebug("Sending hello message to child...")
                    //child.fromParent ("Hello ${childName}")
                    writeLogDebug("Child with id ${childName} successfully created")
                }

                else
                {
                    writeLogDebug("Unable to create child with id ${childName}")
                }
            }

            else
                writeLogDebug("Found child with id ${childName}.")

        }

        childDevices.each{ it ->
            
            //def childDNI = it.deviceNetworkId.split("-")[-1]
            //writeLogDebug("Sending hello message to child ${childDNI}...")
            //it.fromParent ("Hello ${childDNI}")
        }
    }

    catch(e) 
    {
        log.error "Failed to find child without exception: ${e}"
    }    
}

private def getChild(String zoneName)
{
    //writeLogDebug("getChild with ${zoneName}")
    def child = null
    
    try 
    {
        childDevices.each { it ->
            
            //writeLogDebug("child: ${it.deviceNetworkId}")
            if(it.deviceNetworkId == "${device.deviceNetworkId}-${zoneName}")
            {
                child = it
            }
        }
        
        return child
    } 

    catch(e) 
    {
        log.error "getChild caused the following exception: ${e}"
        return null
    }
}

private void createChildDevice(Integer zone, String zoneName, String type) 
{
    writeLogInfo ("Attempting to create child with zoneName ${zoneName} of type ${type}")
    
    try 
    {
        def child = addChildDevice("${type}", "${device.deviceNetworkId}-${zoneName}",
            [label: "Onkyo AVR ${zoneName}",  isComponent: false, name: "${zoneName}"])
        
        writeLogInfo ("Child device with network id: ${device.deviceNetworkId}-${zoneName} successfully created.")
        // Assign the zone number to the child.  The child will use the to filter responses from the AVR
        child.setZone(zone)
    } 

    catch(e) 
    {
        log.error "createChildDevice caused the following exception: ${e}"
    }
}

def fromChild(String msg)
{
    writeLogDebug("Received message from child: ${msg}")
}

String getCommandZoneName(String cmdPrefix)
{
    if(zone1CmdPrefixes.containsValue(cmdPrefix))
    {
        writeLogDebug("${cmdPrefix} belongs to Main Zone")
        return zoneNames[1]
    }

    else if(zone2CmdPrefixes.containsValue(cmdPrefix))
    {
        writeLogDebug("${cmdPrefix} belongs to Zone 2")
        return zoneNames[2]
    }

    else if(zone3CmdPrefixes.containsValue(cmdPrefix))
    {
        writeLogDebug("${cmdPrefix} belongs to Zone 3")
        return zoneNames[3]
    }

    else if(zone4CmdPrefixes.containsValue(cmdPrefix))
    {
        writeLogDebug("${cmdPrefix} belongs to Zone 4")
        return zoneNames[4]        
    }

    else
        writeLogDebug("Unable to determine which zone ${cmdPrefix} is for")

}

def sendTelnetMsg(String msg) 
{
    writeLogDebug("Child called sendTelnetMsg with ${msg}")
    sendHubCommand(new hubitat.device.HubAction(msg, hubitat.device.Protocol.TELNET))
}
 
def getEiscpMessage(command)
{
    def sb = StringBuilder.newInstance()
    def eiscpDataSize = command.length() + 3   // eISCP data size
    def eiscpMsgSize = eiscpDataSize + 1 + 16  // size of the entire eISCP msg

    // Construct the entire message character by character. 
    //Each char is represented by a 2 digit hex value
    sb.append("ISCP")

    // the following are all in HEX representing one char

    // 4 char Big Endian Header
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt("10", 16))

    // 4 char  Big Endian data size
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt(Integer.toHexString(eiscpDataSize), 16))

    // eiscp_version = "01";
    sb.append((char)Integer.parseInt("01", 16))

    // 3 chars reserved = "00"+"00"+"00";
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt("00", 16))
    sb.append((char)Integer.parseInt("00", 16))

    ////////////////////////////////////////
    // eISCP data
    ////////////////////////////////////////

    // Start Character
    sb.append("!")

    // eISCP data - unittype char '1' is receiver
    sb.append("1")

    // eISCP data - 3 char command and param    ie PWR01
    sb.append(command)

    // msg end - this can be a few different chars depending on your receiver
    
    //  [CR]	Carriage Return		ASCII Code 0x0D			
	//  [LF]	Line Feed			ASCII Code 0x0A			
	//  [EOF]	End of File			ASCII Code 0x1A			
	
    // The eISCP protocol lists 4 possible termination options; CR, LF, CRLF, and EOF. The two 
    // receivers models I own don't seem to be all that fussy and work with any of the listed 
    // options.  Regardless, all 4 options are included in the settings in case there are models
    // that require a specific termination character.

    switch(eISCPTermination as Integer)
    {
        case 1:    // CR
            sb.append((char)Integer.parseInt("0D", 16)) 
            writeLogDebug ("->CR")
            break

        case 2:   // LF
            sb.append((char)Integer.parseInt("0A", 16)) 
            writeLogDebug ("->LF")
            break

        case 3:   // CRLF
            sb.append((char)Integer.parseInt("0D", 16)) 
            sb.append((char)Integer.parseInt("0A", 16))
            writeLogDebug ("->CRLR")
            break

        case 4:   // EOF
            sb.append((char)Integer.parseInt("1A", 16)) 
            writeLogDebug ("->EOF")
            break

        default:
            sb.append((char)Integer.parseInt("0D", 16)) 
            writeLogDebug ("Defaulting to CR")
    }

    return sb.toString()
}

def Integer getEiscpVolumeMaxSetting()
{
    Integer maxIscpHexValue = settings?.eISCPVolumeRange?.toBigDecimal()
	writeLogDebug("settings?.eISCPVolumeRange: ${maxIscpHexValue}")

    return maxIscpHexValue
}

def getName()
{
    return device.getName()
}