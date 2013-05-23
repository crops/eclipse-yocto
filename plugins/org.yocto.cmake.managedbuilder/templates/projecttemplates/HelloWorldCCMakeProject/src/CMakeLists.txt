cmake_minimum_required (VERSION 2.8.1)

######## Project settings ########
PROJECT($(projectName))
SET(LICENSE "TBD")

######## Build and include settings ########
include_directories(
	inc
)

link_directories(
	${LINK_DIRECTORIES}
)


file(GLOB SOURCES
	"src/*.c"
)

add_executable(
	$(projectName)

	${SOURCES}
)

TARGET_LINK_LIBRARIES(
	$(projectName)
)

######## Install targets ########
INSTALL(TARGETS $(projectName)
	RUNTIME DESTINATION usr/bin
)
