$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo4j-Management.psm1"

InModuleScope Neo4j-Management {
  Describe "Start-Neo4jServer" {
# Refactor
    Context "Invalid or missing server object" {
      It "throws error for an invalid server object - Server" {
        { Start-Neo4jServer -Server -Neo4jServer (New-Object -TypeName PSCustomObject) -ErrorAction Stop } | Should Throw
      }

      It "throws error for an invalid server object - Console" {
        { Start-Neo4jServer -Console -Neo4jServer (New-Object -TypeName PSCustomObject) -ErrorAction Stop } | Should Throw
      }
    }
    
    # Windows Service Tests
    Context "Missing service name in configuration files" {
      Mock Start-Service { }
      Mock Get-Neo4jWindowsServiceName { throw "Missing Service Name" }

      $serverObject = (New-Object -TypeName PSCustomObject -Property @{
        'Home' =  'TestDrive:\some-dir-that-doesnt-exist';
        'ServerVersion' = '3.0';
        'ServerType' = 'Enterprise';
        'DatabaseMode' = '';
      })      

      It "throws error for missing service name in configuration file" {
        { Start-Neo4jServer -Service -Neo4jServer $serverObject -ErrorAction Stop } | Should Throw
      }
      
      It "calls Get-Neo4jWindowsServiceName" {
        Assert-MockCalled Get-Neo4jWindowsServiceName -Times 1
      }
    }    

    Context "Start service succesfully but not running" {
      Mock Get-Neo4jWindowsServiceName { 'SomeServiceName' }
      Mock Start-Service { throw "Wrong Service name" }
      Mock Start-Service -Verifiable { @{ Status = 'Start Pending'} } -ParameterFilter { $Name -eq 'SomeServiceName'}
      
      $serverObject = (New-Object -TypeName PSCustomObject -Property @{
        'Home' =  'TestDrive:\some-dir-that-doesnt-exist';
        'ServerVersion' = '3.0';
        'ServerType' = 'Enterprise';
        'DatabaseMode' = '';
      })      
      $result = Start-Neo4jServer -Service -Neo4jServer $serverObject

      It "result is 2" {
        $result | Should Be 2
      }
      
      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Start service succesfully" {
      Mock Get-Neo4jWindowsServiceName { 'SomeServiceName' }
      Mock Start-Service { throw "Wrong Service name" }
      Mock Start-Service -Verifiable { @{ Status = 'Running'} } -ParameterFilter { $Name -eq 'SomeServiceName'}
      
      $serverObject = (New-Object -TypeName PSCustomObject -Property @{
        'Home' =  'TestDrive:\some-dir-that-doesnt-exist';
        'ServerVersion' = '3.0';
        'ServerType' = 'Enterprise';
        'DatabaseMode' = '';
      })      
      $result = Start-Neo4jServer -Service -Neo4jServer $serverObject

      It "result is 0" {
        $result | Should Be 0
      }
      
      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    # Console Tests
    Context "Start as a process and missing Java" {
      Mock Get-Java { }
      Mock Start-Process { }

      $serverObject = (New-Object -TypeName PSCustomObject -Property @{
        'Home' =  'TestDrive:\some-dir-that-doesnt-exist';
        'ServerVersion' = '3.0';
        'ServerType' = 'Enterprise';
        'DatabaseMode' = '';
      })      
      It "throws error if missing Java" {
        { Start-Neo4jServer -Console -Neo4jServer $serverObject -ErrorAction Stop } | Should Throw
      }
    }    
  }
}