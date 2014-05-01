LikeLines pipelet for SMILA 1.1.

Computes the top <n> most interesting keyframes of the video denoted by the 
parameter <input_field> using the LikeLines server <server> and stores the 
time-codes of the keyframes as a sequence in the attribute <output_field>.

============
Configuring
============
In order to have the SMILA pipelet invoke content analysis on videos, 
the LLIndexer component needs to be configured. Please see

  configuration/README.txt

for more information.

============
Testing
============
To test the pipelet, copy the BPEL file (LikeLinesPipeline.bpel) to your 
BPEL container folder of SMILA.Application and add the following lines to 
the deploy.xml file:

  <process name="proc:LikeLinesPipeline">
    <in-memory>true</in-memory>
    <provide partnerLink="Pipeline">
      <service name="proc:LikeLinesPipeline" port="ProcessorPort" />
    </provide>    
  </process>

Then, open the Run or Debug Configurations window in Eclipse and check the bundle.

Run SMILA. When it is ready, use a REST client to interact with SMILA:

URL: http://localhost:8080/smila/pipeline/LikeLinesPipeline/process
METHOD: POST
REQUEST BODY:
{
  "Text": "Some record",
  "youtube_id": "YouTube:wPTilA0XxYE"
}

RESULT (example):
    {
       "Text": "Some record",
       "youtube_id": "YouTube:wPTilA0XxYE",
       "_recordid": "LikeLinesPipeline-800097b9-62e4-4a7b-aab4-fba8d0df06c7",
       "topkeyframes":
       [
           63
       ],
       "topkeyframes_jpg":
       [
           "...base64 encoded jpg..."
       ]
    }

Note that the field "topkeyframes_jpg" is only available if the video has been 
indexed and is still in the cache. Also, if frame extraction fails for one of 
the frames, the corresponding entry will be an empty string. 


Alternatively, if you're running SMILA 1.2, you can also test the pipelet 
in isolation using the following REST request:

URL: http://localhost:8080/smila/pipelets/cubrikproject.tud.likelines.pipelets.LikeLines/process
METHOD: POST
REQUEST BODY:
{
  "_configuration": {
    "server": "http://likelines-shinnonoir.dotcloud.com",
    "input_field": "youtube_id",
    "n": 3,
    "output_field": "topkeyframes",
    "output_frames_field": "topkeyframes_jpg"
  },
  "Text": "Some record",
  "youtube_id": "YouTube:wPTilA0XxYE"
}

RESULT (example):
{
  "_configuration": {
    "server": "http://likelines-shinnonoir.dotcloud.com",
    "input_field": "youtube_id",
    "n": 3,
    "output_field": "topkeyframes"
  },
  "Text": "Some record",
  "youtube_id": "YouTube:wPTilA0XxYE",
  "_recordid": "cubrikproject.tud.likelines.pipelets.LikeLines-9e816aa0-3ca9-40de-aaa0-aed857e26291"
  "topkeyframes": [
     63,
     2,
     37
  ],
  "topkeyframes_jpg": [
     "...base64 encoded jpg...",
     "...base64 encoded jpg...",
     "...base64 encoded jpg..."
  ]
}
