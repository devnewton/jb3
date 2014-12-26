<#import "fortune.ftl" as fortuneMacros />
<!DOCTYPE html>
<html>
    <head>
        <title>jb3 fortune</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="robots" content="noindex,nofollow">
        <link rel="stylesheet" type="text/css" href="/jb3-common.css" />
        <link rel="stylesheet" type="text/css" href="/jb3-fortune.css" />
        <link rel="icon" type="image/png" href="/favicon.png" />
    </head>
    <body>
    <div class="jb3-fortunes">
    <#if fortune?? >
        <@fortuneMacros.showFortune fortune />
    <#else>
        Aucune fortune trouvée.
    </#if>
    </div>
    </body>
</html>
