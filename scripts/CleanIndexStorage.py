# Simple script to remove old video files from the index storage
#
# Skip to CONFIGURATION to modify this script's behavior,
# or simply copy this script to the index storage's location
# and execute it from there.
#
# License: MIT
# Author: Raynor Vliegendhart
#
# Requirements: Python 2.6+

import os
import sys
from os.path import getctime, getmtime
from time import sleep
from time import time as now
from glob import iglob as glob
SEC = 1
MINS = 60*SEC
HOURS = 60*MINS
DAYS = 24*HOURS


#===========================================================================
# CONFIGURATION
#===========================================================================

# PATH: The path to monitor for old files
PATH = '.'


# MAX_FILE_AGE: The maximum age of files to retain
MAX_FILE_AGE = 5*DAYS


# INTERVAL: How frequently to scan for old files. 
#           If set to 0, the script only scans once. 
INTERVAL = 1*DAYS


# USE_CREATION_TIME: Use date created if set to True, 
#                    otherwise use date last modified.
USE_CREATION_TIME = False


# PATTERNS: The file patterns to check
PATTERNS = ['mca-???????????.*', 'mca-???????????-conv.mpg']

#===========================================================================



def find_old_files():
    filetime = getctime if USE_CREATION_TIME else getmtime
    
    res = set()
    curtime = now()
    
    for pattern in PATTERNS:
        for filename in glob(os.path.join(PATH, pattern)):
            age = curtime - filetime(filename)
            if age > MAX_FILE_AGE:
                res.add(filename)
    
    return res

def main(INTERVAL=INTERVAL):
    if INTERVAL:
        print >>sys.stderr, 'Press ^C to exit'
    else:
        INTERVAL = 0
    
    try:
        while True:
            print >>sys.stderr, 'Scanning files... ', 
            to_delete = find_old_files()
            print >>sys.stderr, '' if to_delete else 'Nothing found'
            
            for filename in to_delete:
                print >>sys.stderr, '  Deleting "%s"' % filename
                try:
                    os.remove(filename)
                except:
                    print >>sys.stderr, '  Failed to remove "%s"' % filename
            
            if INTERVAL:
                sleep(INTERVAL)
            else:
                break
    
    except KeyboardInterrupt:
        pass
    except:
        print_exc()
    finally:
        print >>sys.stderr, 'Exiting...'


if __name__ == '__main__':
    main()

