import os
import json
import re

templateFilePath = "./gunLootTableTemplate.json"

lootTableDest = "../src/main/resources/data/taczsf/loot_tables/guns/"

gunDataSrc = "../run/tacz/tacz_default_gun/data/tacz/data/guns/"
gunDataSuffix = "_data.json"
gunDataSuffixLen = len(gunDataSuffix)

ammoDataSrc = "../run/tacz/tacz_default_gun/data/tacz/index/ammo/"
ammoDataSuffix = ".json"
ammoDataSuffixLen = len(ammoDataSuffix)

taczPrefix = "tacz:"

# Variables
MIN_MAG_COUNT = 4
MAX_MAG_COUNT = 6

def main():
    # Get ammo stack sizes
    ammoStackSizes = {}
    for filename in os.scandir(ammoDataSrc):
        # Ignore errors (hieroglyphs)
        with open(filename.path, "r", errors="ignore") as ammoDataFile:
            # Remove comments (invalid json)
            fixedAmmoDataStr = re.sub('(//.*)|(/\\*(.|\n)*\\*/)', '', ammoDataFile.read())
            ammoData = json.loads(fixedAmmoDataStr)

            ammoStackSizes[taczPrefix + str(filename.name[0:-ammoDataSuffixLen])] = ammoData["stack_size"]

    templateFile = open(templateFilePath, "r")
    template = templateFile.read()
    templateFile.close()

    # Generate gun loot tables
    for filename in os.scandir(gunDataSrc):
        # Ignore errors (hieroglyphs)
        with open(gunDataSrc + "ak47_data.json", "r", errors="ignore") as gunDataFile:
            # Remove comments (invalid json)
            fixedGunDataStr = re.sub('(//.*)|(/\\*(.|\n)*\\*/)', '', gunDataFile.read())
            gunData = json.loads(fixedGunDataStr)

            gunName = filename.name[0:-gunDataSuffixLen]
            magazineSize = gunData["ammo_amount"]

            # Nothing breaks if we set fire mode to `burst` for every gun, otherwise:
            # GunFireMode: "BURST" if "burst" in gunData["fire_mode"] else "SEMI",
            _gunNbt = ('\"{' + f"""
                GunId: \\"{taczPrefix + gunName}\\",
                GunFireMode: \\"BURST\\",
                gunCurrentAmmoCount: {magazineSize}
            """ + '}\"').replace('\n', '').replace(' ', '')

            ammoId = gunData["ammo"]

            _ammoNbt = ('\"{' + f"""
                AmmoId: \\"{ammoId}\\"
            """ + '}\"').replace('\n', '').replace(' ', '')

            _ammoCountMin = ammoStackSizes[ammoId] * 0.25
            _ammoCountMax = ammoStackSizes[ammoId] * 1
            # We want enough rolls so that minimum ammo count is guaranteed
            _ammoRollCount = MIN_MAG_COUNT * magazineSize / _ammoCountMin

            gunLootTable = template % locals()

            outputFilePath = lootTableDest + gunName + ".json"
            with open(outputFilePath, "w") as outputFile:
                print("[INFO] Writing %s" % outputFilePath)
                outputFile.write(gunLootTable)

if __name__ == '__main__':
    main()