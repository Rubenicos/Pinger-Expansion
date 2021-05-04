# Pinger-Expansion
This expansion is a recoded version from original one made by clip

## Placeholder
This placeholder has different ways to use

**Simple way:**

`%pinger_<data-type>_<ip>%` Ping server IP

`%pinger_<data-type>_<ip>:[port]%` Ping server IP with port

**Complex way:**

`%pinger_<data-type>_<ip>:[port]:[timeout]%` Ping server with custom timeout

`%pinger_<data-type>_<ip>:[port]:[timeout]_[refresh]%` To make a custom refresh interval in seconds

## Data Types
* `pingversion | ping` - Server ping version
* `protocolversion | protocolv` - Server protocol version
* `gameversion | version` - Server game version
* `motd` - Server motd
* `count | players` - Number of players online
* `max | maxplayers` - Maximum number of players
* `online` - Check if server are online

## Recoded Version
What's new in this recoded version?
* **Different config file:** Now Pinger config location is at plugins/PlaceholderAPI/expansions/Pinger/settings.yml
* **Custom times:** You can set your own timeout and refresh interval per ip ping
* **Cache requests:** Option to cache the placeholder by putting `_cache:[seconds]` at final.
* **IP tables:** Get data from multiple IPs at the same time (with the option to make a sum).
* **Web API:** Get data using a web API instead default method.

You can find all the necessary information about these new options on [wiki](https://github.com/Rubenicos/Pinger-Expansion/wiki).
