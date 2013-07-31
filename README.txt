LikeLines pipelet.

Computes the top <n> most interesting keyframes of the video denoted by the 
parameter <input_field> using the LikeLines server <server> and stores the 
time-codes of the keyframes as a sequence in the attribute <output_field>.


EXAMPLE CONFIGURATION:

    <extensionActivity>
      <proc:invokePipelet name="invokeLikeLines">
        <proc:pipelet class="cubrikproject.tud.likelines.pipelets.LikeLines" />
        <proc:variables input="request" output="request" />
        <proc:configuration>
          <rec:Val key="server">http://likelines-shinnonoir.dotcloud.com</rec:Val>
          <rec:Val key="input_field">youtube_id</rec:Val>
          <rec:Val key="n">1</rec:Val>
          <rec:Val key="output_field">topkeyframes</rec:Val>
        </proc:configuration>
      </proc:invokePipelet>
    </extensionActivity>


EXAMPLE REST QUERY FOR TESTING:

POST http://localhost:8080/smila/pipelets/cubrikproject.tud.likelines.pipelets.LikeLines/process
{
  "_configuration": {
    "server": "http://likelines-shinnonoir.dotcloud.com",
    "input_field": "youtube_id",
    "n": 1,
    "output_field": "topkeyframes"
  },
  "Text": "Some record",
  "youtube_id": "YouTube:wPTilA0XxYE"
}

==>

{
  "_configuration": {
    "server": "http://likelines-shinnonoir.dotcloud.com",
    "input_field": "youtube_id",
    "n": 1,
    "output_field": "topkeyframes"
  },
  "Text": "Some record",
  "youtube_id": "YouTube:wPTilA0XxYE",
  "_recordid": "cubrikproject.tud.likelines.pipelets.LikeLines-9e816aa0-3ca9-40de-aaa0-aed857e26291"
  "topkeyframes": [
    63.00
  ]
}

