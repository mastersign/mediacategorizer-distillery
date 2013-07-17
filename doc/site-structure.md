Distillery Site Structure
=========================

## Index
Path: `/index.html`
### Context
* Multiple Categories
* Multiple Videos
### Views
* Index
	* Description
	* Statistics
* Categories
	* List -> Category
* Videos
	* List -> Video
* Glossary
	* List -> Index:Word
	* Cloud -> Index:Word
* Word*  
  Path: `/words/<word>.part.html`
	* Statistics
	* Categories (subset) -> Category
	* Videos (subset) -> Video

## Category*
Path: `/categories/<category>/index.html`
### Context
* Single Category
* Multiple Videos (subset)
### Views
* Category
	* Description
	* Statistics
* Videos
	* List -> Video
* Glossary
	* List -> Category:Word
	* Cloud -> Category:Word
* Word*  
  Path: `/categories/<category>/words/<word>.part.html`
	* Statistics
	* Videos (subset) -> Video

## Video*
Path: `/videos/<video>/index.html`
### Context
* Single Video
* Multiple Categories (subset)
### Views
* Video
	* Description
	* Statistics
* Phrases
	* Transcript -> Playback Position, Video:Word
* Categories
	* List -> Category
* Glossary
	* List -> Video:Word
	* Cloud -> Video:Word
* Word*  
  Path: `/videos/<video>/words/<word>.part.html`
	* Statistics
	* Occurrences -> Playback Position
