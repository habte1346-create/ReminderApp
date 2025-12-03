const express = require('express');
const cors = require('cors');
const app = express();
app.use(cors());

app.get('/key', (req, res) => {
    // Railway will provide this variable
    res.json({ key: process.env.GROQ_API_KEY });
});

app.listen(process.env.PORT || 3000);
