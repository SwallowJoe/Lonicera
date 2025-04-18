# Lonicera

## ParcelFileDescriptor.createSocketPair

创建套接字对，返回一个数组，数组的第一个元素是服务器端套接字，第二个元素是客户端套接字。

注意，返回的fd 是 ParcelFileDescriptor 类型，而不是 FileDescriptor 类型。

另外这个fd通过binder传输后，本地端会自动关闭！！！