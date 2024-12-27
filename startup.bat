title IEPC_MJ
chcp 65001
for %%j in (iot*.jar) do java -jar -Dfile.encoding=utf-8 %%j 