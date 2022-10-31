# To run this Powershell Script, you need to run Powershell as admin and run "set-executionpolicy remotesigned"
# "The RemoteSigned execution policy requires a digital signature on scripts that you download
# or get from other computers, but it does not require a digital signature on scripts that you 
# write on your local computer." See: https://superuser.com/a/106363
$wildfly_zip = 'wildfly-25.0.0.Final.zip'
$wildfly_basename = 'wildfly-25.0.0.Final'
$url = 'https://github.com/wildfly/wildfly/releases/download/25.0.0.Final/wildfly-25.0.0.Final.zip'
$wildfly_current = 'wildfly-current'
$tmp = 'c:\tmp' # This should be short to avoid "filename too long"-errors
If(!(test-path $tmp))
{
      New-Item -ItemType Directory -Force -Path $tmp
}

Remove-Item $wildfly_current -Recurse
# "wget" maps to a power-shell internal function, no need for GNU wget
$progressPreference = 'silentlyContinue'; wget $url -OutFile $wildfly_zip

$shell_app=new-object -com shell.application
$zip_file = $shell_app.namespace((Get-Location).Path + "\$wildfly_zip")

# Extract to short tmp dir to avoid "filename too long"-errors
$destination = $shell_app.namespace($tmp)

# This extracts the zipfile recursively
$destination.Copyhere($zip_file.items())

# Move back to working dir rename and clean up
Move-Item "$tmp\$wildfly_basename" (Get-Location).Path
Rename-Item $wildfly_basename $wildfly_current
Remove-Item $wildfly_zip