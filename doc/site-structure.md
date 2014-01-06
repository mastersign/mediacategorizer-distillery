Distillery Site Structure
=========================

## Index
Path: `/index.html`
### Context
* Multiple Categories
* Multiple Videos
### Views
* Project
	* Overview
		* Description
		* Statistics
		* Word Hitlist &rarr; Project:Word
	* Cloud
		* Cloud Word &rarr; Project:Word 
	* Matrix
		* Row &rarr; Video:Overview
		* Column &rarr;> Category:Overview
		* Cell &rarr; Category:Match 
	* Glossary
* Categories
	* Overview 
		* List &rarr; Category:Overview
* Videos
	* Overview	
		* List &rarr; Video:Overview
* Word*  
  Path: `/words/<word>.inc.html`
	* Statistics
	* Videos (subset) &rarr; Video:Word
	* Categories (subset) &rarr; Category:Word

## Category*
Path: `/categories/<category>/index.html`
### Context
* Single Category
* Multiple Videos (subset)
### Views
* Category (Overview)
	* Description
	* Statistics
	* Word Hitlist &rarr; Category:Word
* Cloud
	* Cloud Word &rarr; Category:Word
* Videos
	* List &rarr; Category:Match
* Glossary
	* List &rarr; Category:Word
* Word*  
  Path: `/categories/<category>/words/<word>.inc.html`
	* Statistics
	* Videos (subset) &rarr; Video:Word
* Match*  
  Path: `/categories/<category>/matches/<video>.inc.html`
	* Match Score
	* Word List &rarr; Category:Word

## Video*
Path: `/videos/<video>/index.html`  
Video-Path: `/video/<video>/<video>.mp4`  
Waveform-Path: `/videos/<video>/waveform.png`
### Context
* Single Video
* Multiple Categories (subset)
### Views
* Video (Overview)
	* Description
	* Statistics
	* Waveform
	* Word Hitlist
* Cloud
	* Cloud Word &rarr; Video:Word 
* Transcript
	* Phrase Time &rarr; Playback Position
	* Phrase Word &rarr; Video:Word
* Categories
	* List Item &rarr; Category:Match
* Glossary
	* List Item &rarr; Video:Word
* Word*  
  Path: `/videos/<video>/words/<word>.inc.html`
	* Statistics
	* Occurrences &rarr; Playback Position
	* Categories &rarr; Category:Word
