<html>
<head>
    <script type="text/javascript" language="javascript">
        //<![CDATA[
        function generateQueryString()
        {
            var href = window.location.href;
            var queryString = "";

            var currentQueryString = "";
            var hashQueryString = "";
            if(href.indexOf('?') != -1){
                if(href.indexOf('#') != -1){
                    currentQueryString = href.slice(href.indexOf('?') + 1, href.indexOf('#'));
                } else {
                    currentQueryString = href.slice(href.indexOf('?') + 1);
                }
            }

            if(href.indexOf('#') != -1){
                hashQueryString = href.slice(href.indexOf('#') + 1);
            }

            if(currentQueryString != "" || hashQueryString != ""){
                if(currentQueryString != ""){
                    queryString = "?" + currentQueryString;
                    if(hashQueryString != ""){
                        queryString += "&" + hashQueryString;
                    }
                } else {
                    queryString = "?" + hashQueryString;
                }
            }

            return queryString;
        }

        function redirect(){
            var loc = "<%=request.getContextPath() + "/Authn/RemoteUser"%>" + generateQueryString();
            window.location = loc;
        }

        // ]]>
    </script>
</head>
<body onLoad="redirect()"></body>
</html>
