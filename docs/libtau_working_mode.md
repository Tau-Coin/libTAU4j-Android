# 工况模式
* 充电、计费、IPv4、IPv6、流量、Processors由系统采集；
* Invoke、CPU、Memory为经验最大值；
* 默认情况下按模式14运行：
	- Processors默认值为4；
	- 充电默认值为Y；
	- 计费默认值为Y；
	- IP默认IPv4为Y, IPv6为N；
	- 流量计费默认为100MB，非计费为500MB；

<table>
	<tr>
		<th>模式</th>
		<th>充电（Y/N）</th>
		<th>计费（Y/N）</th>
		<th>IPv4（Y/N）</th>
		<th>IPv6（Y/N）</th>
		<th>流量(MB)</th>
		<th>Invoke</th>
		<th>CPU/Processors（%）</th>
		<th>Memory(MB)</th>
	</tr>
	<tr>
		<td>1</td>
		<td rowspan="9">Y</td>
		<td rowspan="9">Y</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">Y</td>
		<td>30</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>2</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>3</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>4</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">N</td>
		<td>30</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>5</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>6</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>7</td>
		<td rowspan="3">N</td>
		<td rowspan="3">Y</td>
		<td>30</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>8</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>9</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>10</td>
		<td rowspan="9">N</td>
		<td rowspan="9">Y</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">Y</td>
		<td>30</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>11</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>12</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>13</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">N</td>
		<td>30</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>14</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>15</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>16</td>
		<td rowspan="3">N</td>
		<td rowspan="3">Y</td>
		<td>30</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>17</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>18</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>19</td>
		<td rowspan="9">Y</td>
		<td rowspan="9">N</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">Y</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>20</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>21</td>
		<td>1024</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>22</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">N</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>23</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>24</td>
		<td>1024</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>25</td>
		<td rowspan="3">N</td>
		<td rowspan="3">Y</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>26</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>27</td>
		<td>1024</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>28</td>
		<td rowspan="9">N</td>
		<td rowspan="9">N</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">Y</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>29</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>30</td>
		<td>1024</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>31</td>
		<td rowspan="3">Y</td>
		<td rowspan="3">N</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>32</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>33</td>
		<td>1024</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>34</td>
		<td rowspan="3">N</td>
		<td rowspan="3">Y</td>
		<td>100</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>35</td>
		<td>500</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>36</td>
		<td>1024</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
</table>