# Android 网络IPv4获取方案
第一种方案：

采用ConnectivityManager.getAllNetworks()和ConnectivityManager.getNetworkInfo(NetWork network)来获取解析网络IPv4, 但是ConnectivityManager.getAllNetworks()在Android API 31中已弃用，官方推荐
ConnectivityManager.registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback)来替代使用。

此方案在大多数手机和网络环境中测试使用正常；但在英国的网络环境中测试无法获取。分析使用ConnectivityManager.getAllNetworks()获取的是系统服务ConectivityService中跟踪的所有网络，而有的网络环境中无法获取到IPv4所在的Interface（如v4-rmnet0）。


第二种方案：

使用NetworkInterface.getNetworkInterfaces()来获取Android设备上的所有的网络接口，然后遍历获取解析网络出IPv4。可以解决第一种方案中的不足。

由于应用主要想获取当前Active的network, 所以需要两种方案结合使用：优先使用第一种方案，如获取不到IPv4, 再使用第二种方案获取。