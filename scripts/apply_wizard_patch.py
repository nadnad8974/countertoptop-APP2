from pathlib import Path
import base64
import gzip
import subprocess

main_path = Path("app/src/main/java/com/ramsiers/graniteapp/MainActivity.java")
build_path = Path("app/build.gradle")
marker = "navigation.addView(share, shareParams);"
version = "versionName '1.18-test'"

patch_gzip_base64 = "H4sIAAAAAAACA81WUW/aSBB+51fM8RIjbIPTJIBOPbWJqqQ6mkQF9bVaewezh9m1dhfS6NT/3lkbbCc4bY6+nIXwepjv2/nmG9twsVhAEKTCAhuwPB/EG5HxMNWMZwjxQagjJMdvwCfn0fAsCcOI4WhyihANhxdnZ50gCFp4Ov1+v43r3TsIoqE/hn7xTZdMcq0Eh387sD8IlYmEWaHkRw4niVqHmq2NQG0cjxQWKeWkBqyFnPEVnL6pQ5bpFK2LvjnvBPvoliiI9Upxqn90EL9la4STKIwuAovGnnT6rcDxQbwCjnbA4sfvHSgXpCAXGd7lTpIhqfy5B0YngzUTcvAP27IB5Q/2ige14sEnynifWLEV9jF0mbsWH4neOXuaRJwteBieno/5mLc4eyx/PQTHMrh5GV0M/RH0yxMF8k1M4wFJxoyBJgTwm0XJDVSBxlDtj+6lSCHGLWaQLJVIsOsfJiFP0XnqdWORfi2yu34D2u31/iRzq/m53FirJBi2RXgLuRZrph/LoNeduSiNOcVxK/ChS9j+q5Euu6rLpYUG7Z28ohaspsKQYNTeFoK/nosVC/D+cICp2OIHLqzSX1i2QeMd6i2GdGPVxt5raolfNGC3XLBNgvt4zMyquki14OVy59PEnzifJuWdfbxPZqkeZhZzr6ndHXPFjA3XbIVz4vDsUhiy5X2ek5dMpmiKDvGQzCpTpx9ur+c3X2c3d5/nvdDxPuH8TheViTlLMWScfyGPPMfT7vGSaWeVwURJ3jSLJFVlWAVXS2av7+ewUBquhb3ZxK3G/4zuqfcu8/9r/mjiRxG5Pxr70W/avxQc/8bHWDHND0agaEOt6qps+GtddeBWWxMmE3oktBhxyZIVPAi7pAa58RIy7TY3KJEvGlPP8otVlQwtwxG7rV8o6ckwucTXVVBBJClJi3dsVYdj8UHiA0yFRKan7JE0h+Xpnrkntjf0gefe+VnPh2hRENaUL6KKu3K3fPufNmgUXHM4pZ/o/S6k8Sj3glKH5ecXAh2F3yB6dflubH67/prkeAGOw29SPXlCJFplWZgrYz2vV1hfRsxaKbucFRdz5RUb7YHuT8oP+83NAxMKAAA="

main = main_path.read_text()
build = build_path.read_text()
if marker in main and version in build:
    print("v1.18 product selections already applied.")
else:
    patch = gzip.decompress(base64.b64decode(patch_gzip_base64))
    result = subprocess.run(
        ["git", "apply", "--whitespace=nowarn", "-"],
        input=patch,
        capture_output=True,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    print("Applied v1.17 fixed editor controls.")
